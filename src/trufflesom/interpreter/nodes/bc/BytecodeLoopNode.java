package trufflesom.interpreter.nodes.bc;

import static trufflesom.interpreter.bc.Bytecodes.DUP;
import static trufflesom.interpreter.bc.Bytecodes.HALT;
import static trufflesom.interpreter.bc.Bytecodes.POP;
import static trufflesom.interpreter.bc.Bytecodes.POP_ARGUMENT;
import static trufflesom.interpreter.bc.Bytecodes.POP_FIELD;
import static trufflesom.interpreter.bc.Bytecodes.POP_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_ARGUMENT;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_BLOCK;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_CONSTANT;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_FIELD;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_GLOBAL;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.Q_PUSH_GLOBAL;
import static trufflesom.interpreter.bc.Bytecodes.Q_SEND;
import static trufflesom.interpreter.bc.Bytecodes.RETURN_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.RETURN_NON_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.SEND;
import static trufflesom.interpreter.bc.Bytecodes.SUPER_SEND;
import static trufflesom.interpreter.bc.Bytecodes.getBytecodeLength;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.ExplodeLoop.LoopExplosionKind;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.profiles.ValueProfile;

import trufflesom.interpreter.EscapedBlockException;
import trufflesom.interpreter.FrameOnStackMarker;
import trufflesom.interpreter.Invokable;
import trufflesom.interpreter.ReturnException;
import trufflesom.interpreter.SArguments;
import trufflesom.interpreter.Types;
import trufflesom.interpreter.bc.RestartLoopException;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.GlobalNode;
import trufflesom.interpreter.nodes.MessageSendNode;
import trufflesom.interpreter.nodes.MessageSendNode.GenericMessageSendNode;
import trufflesom.interpreter.nodes.dispatch.CachedDnuNode;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SAbstractObject;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


public class BytecodeLoopNode extends ExpressionNode {
  private static final ValueProfile frameType = ValueProfile.createClassProfile();

  @CompilationFinal(dimensions = 1) private final byte[]      bytecodes;
  @CompilationFinal(dimensions = 1) private final FrameSlot[] localsAndOuters;
  @CompilationFinal(dimensions = 1) private final Object[]    literalsAndConstants;

  @Children private ExpressionNode[] quickened;

  private final IndirectCallNode indirectCallNode;

  private final int      numLocals;
  private final int      maxStackDepth;
  private final Universe universe;

  private final FrameSlot frameOnStackMarker;

  public BytecodeLoopNode(final byte[] bytecodes, final int numLocals,
      final FrameSlot[] localsAndOuters,
      final Object[] literals, final int maxStackDepth, final FrameSlot frameOnStackMarker,
      final Universe universe) {
    this.bytecodes = bytecodes;
    this.numLocals = numLocals;
    this.localsAndOuters = localsAndOuters;
    this.literalsAndConstants = literals;
    this.maxStackDepth = maxStackDepth;
    this.universe = universe;
    this.indirectCallNode = Truffle.getRuntime().createIndirectCallNode();

    this.frameOnStackMarker = frameOnStackMarker;
  }

  @ExplodeLoop
  private VirtualFrame determineOuterContext(final VirtualFrame frame) {
    // TODO: change bytecode format to include the context level
    Object object = frame.getArguments()[0];

    if (!(object instanceof SBlock)) {
      return frame;
    }

    SBlock self = (SBlock) object;
    MaterializedFrame outer = self.getContext();

    while (true) {
      Object rcvr = outer.getArguments()[0];

      if (rcvr instanceof SBlock) {
        outer = ((SBlock) rcvr).getContext();
      } else {
        return outer;
      }
    }
  }

  @ExplodeLoop
  private MaterializedFrame determineContext(final VirtualFrame frame,
      final int contextLevel) {
    SBlock self = (SBlock) frame.getArguments()[0];
    int i = contextLevel - 1;

    while (i > 0) {
      self = (SBlock) self.getOuterSelf();
      i--;
    }

    // Graal needs help here to see that this is always a MaterializedFrame
    // so, we record explicitly a class profile
    return frameType.profile(self.getContext());
  }

  @Override
  @ExplodeLoop(kind = LoopExplosionKind.MERGE_EXPLODE)
  public Object executeGeneric(final VirtualFrame frame) {
    Object[] stack = new Object[maxStackDepth];
    int stackPointer = -1;
    int bytecodeIndex = 0;

    while (true) {
      byte bytecode = bytecodes[bytecodeIndex];
      final int bytecodeLength = getBytecodeLength(bytecode);
      int nextBytecodeIndex = bytecodeIndex + bytecodeLength;

      CompilerAsserts.partialEvaluationConstant(bytecodeIndex);
      CompilerAsserts.partialEvaluationConstant(bytecode);

      switch (bytecode) {
        case HALT: {
          return stack[stackPointer];
        }

        case DUP: {
          Object top = stack[stackPointer];
          stackPointer += 1;
          stack[stackPointer] = top;
          break;
        }

        case PUSH_LOCAL: {
          byte localIdx = bytecodes[bytecodeIndex + 1];
          byte contextIdx = bytecodes[bytecodeIndex + 2];

          VirtualFrame currentOrContext = frame;
          if (contextIdx > 0) {
            currentOrContext = determineContext(currentOrContext, contextIdx);
          }
          FrameSlot slot = localsAndOuters[localIdx];

          Object value = currentOrContext.getValue(slot);
          stackPointer += 1;
          stack[stackPointer] = value;
          break;
        }

        case PUSH_ARGUMENT: {
          byte argIdx = bytecodes[bytecodeIndex + 1];
          byte contextIdx = bytecodes[bytecodeIndex + 2];

          VirtualFrame currentOrContext = frame;
          if (contextIdx > 0) {
            currentOrContext = determineContext(currentOrContext, contextIdx);
          }

          Object value = currentOrContext.getArguments()[argIdx];
          stackPointer += 1;
          stack[stackPointer] = value;
          break;
        }

        case PUSH_FIELD: {
          byte fieldIdx = bytecodes[bytecodeIndex + 1];
          byte contextIdx = bytecodes[bytecodeIndex + 2];

          VirtualFrame currentOrContext = frame;
          if (contextIdx > 0) {
            currentOrContext = determineContext(currentOrContext, contextIdx);
          }

          Object value = ((SObject) currentOrContext.getArguments()[0]).getField(fieldIdx);
          stackPointer += 1;
          stack[stackPointer] = value;
          break;
        }

        case PUSH_BLOCK: {
          byte literalIdx = bytecodes[bytecodeIndex + 1];
          SMethod blockMethod = (SMethod) literalsAndConstants[literalIdx];

          Object value = new SBlock(blockMethod,
              universe.getBlockClass(blockMethod.getNumberOfArguments()), frame.materialize());
          stackPointer += 1;
          stack[stackPointer] = value;
          break;
        }

        case PUSH_CONSTANT: {
          byte literalIdx = bytecodes[bytecodeIndex + 1];

          Object value = literalsAndConstants[literalIdx];
          stackPointer += 1;
          stack[stackPointer] = value;
          break;
        }

        case PUSH_GLOBAL: {
          CompilerDirectives.transferToInterpreterAndInvalidate();

          byte literalIdx = bytecodes[bytecodeIndex + 1];
          SSymbol globalName = (SSymbol) literalsAndConstants[literalIdx];

          GlobalNode quickened = GlobalNode.create(globalName, universe, sourceSection);
          quickenBytecode(bytecodeIndex, Q_PUSH_GLOBAL, quickened);

          // TODO: what's the correct semantics here? the outer or the closed self? normally,
          // I'd expect the outer
          // determineOuterContext(frame);

          stackPointer += 1;
          stack[stackPointer] = quickened.executeGeneric(frame);
          break;
        }

        case POP: {
          stackPointer -= 1;
          break;
        }

        case POP_LOCAL: {
          byte localIdx = bytecodes[bytecodeIndex + 1];
          byte contextIdx = bytecodes[bytecodeIndex + 2];

          VirtualFrame currentOrContext = frame;
          if (contextIdx > 0) {
            currentOrContext = determineContext(currentOrContext, contextIdx);
          }

          FrameSlot slot = localsAndOuters[localIdx];

          Object value = stack[stackPointer];
          stackPointer -= 1;

          currentOrContext.setObject(slot, value);
          break;
        }

        case POP_ARGUMENT: {
          byte argIdx = bytecodes[bytecodeIndex + 1];
          byte contextIdx = bytecodes[bytecodeIndex + 2];

          VirtualFrame currentOrContext = frame;
          if (contextIdx > 0) {
            currentOrContext = determineContext(currentOrContext, contextIdx);
          }

          Object value = stack[stackPointer];
          stackPointer -= 1;

          currentOrContext.getArguments()[argIdx] = value;
          break;
        }

        case POP_FIELD: {
          byte fieldIdx = bytecodes[bytecodeIndex + 1];
          byte contextIdx = bytecodes[bytecodeIndex + 2];

          VirtualFrame currentOrContext = frame;
          if (contextIdx > 0) {
            currentOrContext = determineContext(currentOrContext, contextIdx);
          }

          Object value = stack[stackPointer];
          stackPointer -= 1;

          ((SObject) currentOrContext.getArguments()[0]).setField(fieldIdx, value);
          break;
        }

        case SEND: {
          CompilerDirectives.transferToInterpreterAndInvalidate();
          try {
            byte literalIdx = bytecodes[bytecodeIndex + 1];
            SSymbol signature = (SSymbol) literalsAndConstants[literalIdx];
            int numberOfArguments = signature.getNumberOfSignatureArguments();

            GenericMessageSendNode quickened =
                MessageSendNode.createGeneric(signature, null, sourceSection, universe);
            quickenBytecode(bytecodeIndex, Q_SEND, quickened);

            Object[] callArgs = new Object[numberOfArguments];
            System.arraycopy(stack, stackPointer - numberOfArguments + 1, callArgs, 0,
                numberOfArguments);
            stackPointer -= numberOfArguments;

            Object result = quickened.doPreEvaluated(frame, callArgs);

            stackPointer += 1;
            stack[stackPointer] = result;
          } catch (RestartLoopException e) {
            nextBytecodeIndex = 0;
            stackPointer = -1;
          } catch (EscapedBlockException e) {
            CompilerDirectives.transferToInterpreter();
            VirtualFrame outer = determineOuterContext(frame);
            SObject sendOfBlockValueMsg = (SObject) outer.getArguments()[0];
            Object result =
                SAbstractObject.sendEscapedBlock(sendOfBlockValueMsg, e.getBlock(), universe);

            stackPointer += 1;
            stack[stackPointer] = result;
          }
          break;
        }

        case SUPER_SEND: {
          try {
            byte literalIdx = bytecodes[bytecodeIndex + 1];
            SSymbol signature = (SSymbol) literalsAndConstants[literalIdx];
            int numberOfArguments = signature.getNumberOfSignatureArguments();
            Object[] callArgs = new Object[numberOfArguments];
            System.arraycopy(stack, stackPointer - numberOfArguments + 1, callArgs, 0,
                numberOfArguments);
            stackPointer -= numberOfArguments;

            Object result = doSuperSend(signature, callArgs);

            stackPointer += 1;
            stack[stackPointer] = result;
          } catch (RestartLoopException e) {
            nextBytecodeIndex = 0;
            stackPointer = -1;
          } catch (EscapedBlockException e) {
            CompilerDirectives.transferToInterpreter();
            VirtualFrame outer = determineOuterContext(frame);
            SObject sendOfBlockValueMsg = (SObject) outer.getArguments()[0];

            Object result =
                SAbstractObject.sendEscapedBlock(sendOfBlockValueMsg, e.getBlock(), universe);

            stackPointer += 1;
            stack[stackPointer] = result;
          }
          break;
        }

        case RETURN_LOCAL: {
          return stack[stackPointer];
        }

        case RETURN_NON_LOCAL: {
          Object result = stack[stackPointer];
          // stackPointer -= 1;
          doReturnNonLocal(frame, bytecodeIndex, result);
          break;
        }

        case Q_PUSH_GLOBAL: {
          stackPointer += 1;
          stack[stackPointer] = quickened[bytecodeIndex].executeGeneric(frame);
          break;
        }

        case Q_SEND: {
          GenericMessageSendNode node = (GenericMessageSendNode) quickened[bytecodeIndex];

          int numberOfArguments =
              node.getInvocationIdentifier().getNumberOfSignatureArguments();

          Object[] callArgs = new Object[numberOfArguments];
          System.arraycopy(stack, stackPointer - numberOfArguments + 1, callArgs, 0,
              numberOfArguments);
          stackPointer -= numberOfArguments;

          try {
            Object result = node.doPreEvaluated(frame, callArgs);

            stackPointer += 1;
            stack[stackPointer] = result;
          } catch (RestartLoopException e) {
            nextBytecodeIndex = 0;
            stackPointer = -1;
          } catch (EscapedBlockException e) {
            CompilerDirectives.transferToInterpreter();
            VirtualFrame outer = determineOuterContext(frame);
            SObject sendOfBlockValueMsg = (SObject) outer.getArguments()[0];
            Object result =
                SAbstractObject.sendEscapedBlock(sendOfBlockValueMsg, e.getBlock(), universe);

            stackPointer += 1;
            stack[stackPointer] = result;
          }

          break;
        }

        default:
          Universe.errorPrintln("Nasty bug in interpreter");
          break;
      }

      bytecodeIndex = nextBytecodeIndex;
    }
  }

  private void quickenBytecode(final int bytecodeIndex, final byte quickenedBytecode,
      final ExpressionNode quickenedNode) {
    if (this.quickened == null) {
      this.quickened = new ExpressionNode[bytecodes.length];
    }
    this.quickened[bytecodeIndex] = insert(quickenedNode);
    bytecodes[bytecodeIndex] = quickenedBytecode;
  }

  private SClass getHolder() {
    return ((Invokable) getRootNode()).getHolder();
  }

  private Object doSuperSend(final SSymbol signature, final Object[] callArgs) {
    SClass holderSuper = (SClass) getHolder().getSuperClass();
    SInvokable invokable = holderSuper.lookupInvokable(signature);

    return performInvoke(signature, invokable, callArgs);
  }

  private Object performInvoke(final SSymbol signature, final SInvokable invokable,
      final Object[] callArgs) {
    if (invokable != null) {
      return invokable.invoke(indirectCallNode, callArgs);
    } else {
      CompilerDirectives.transferToInterpreter();
      SArray argumentsArray = SArguments.getArgumentsWithoutReceiver(callArgs);
      CallTarget callTarget = CachedDnuNode.getDnuCallTarget(getHolder(), universe);
      return indirectCallNode.call(callTarget, callArgs[0], signature, argumentsArray);
    }
  }

  private void doReturnNonLocal(final VirtualFrame frame, final int bytecodeIndex,
      final Object result) {
    byte contextIdx = bytecodes[bytecodeIndex + 1];

    MaterializedFrame ctx = determineContext(frame, contextIdx);
    FrameOnStackMarker marker =
        (FrameOnStackMarker) FrameUtil.getObjectSafe(ctx, frameOnStackMarker);

    if (marker.isOnStack()) {
      throw new ReturnException(result, marker);
    } else {
      SBlock block = (SBlock) SArguments.rcvr(frame);
      throw new EscapedBlockException(block);
    }
  }

  private Object doSend(final SSymbol signature, final Object[] callArgs) {
    SInvokable invokable = doLookup(signature, callArgs);
    return performInvoke(signature, invokable, callArgs);
  }

  @TruffleBoundary
  private SInvokable doLookup(final SSymbol signature, final Object[] callArgs) {
    SClass rcvrClass = Types.getClassOf(callArgs[0], universe);
    SInvokable invokable = rcvrClass.lookupInvokable(signature);
    return invokable;
  }

  public int getNumberOfLocals() {
    return numLocals;
  }

  public int getMaximumNumberOfStackElements() {
    return maxStackDepth;
  }

  public int getNumberOfBytecodes() {
    return bytecodes.length;
  }

  public byte getBytecode(final int idx) {
    return bytecodes[idx];
  }

  public Object getConstant(final int idx) {
    return literalsAndConstants[idx];
  }

}
