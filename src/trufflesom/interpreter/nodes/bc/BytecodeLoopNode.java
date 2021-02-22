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
import static trufflesom.interpreter.bc.Bytecodes.RETURN_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.RETURN_NON_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.SEND;
import static trufflesom.interpreter.bc.Bytecodes.SUPER_SEND;
import static trufflesom.interpreter.bc.Bytecodes.getBytecodeLength;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.profiles.ValueProfile;

import trufflesom.interpreter.FrameOnStackMarker;
import trufflesom.interpreter.Invokable;
import trufflesom.interpreter.ReturnException;
import trufflesom.interpreter.SArguments;
import trufflesom.interpreter.Types;
import trufflesom.interpreter.bc.Frame;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.dispatch.CachedDnuNode;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SAbstractObject;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SSymbol;


public class BytecodeLoopNode extends ExpressionNode {
  private static final ValueProfile frameType = ValueProfile.createClassProfile();

  @CompilationFinal(dimensions = 1) private final byte[]      bytecodes;
  @CompilationFinal(dimensions = 1) private final FrameSlot[] locals;
  @CompilationFinal(dimensions = 1) private final Object[]    literalsAndConstants;

  private final IndirectCallNode indirectCallNode;

  private final int      maxStackDepth;
  private final Universe universe;

  private final FrameSlot stackVar;
  private final FrameSlot stackPointer;
  private final FrameSlot frameOnStackMarker;

  public BytecodeLoopNode(final byte[] bytecodes, final FrameSlot[] locals,
      final Object[] literals, final int maxStackDepth, final FrameSlot stackVar,
      final FrameSlot stackPointer, final FrameSlot frameOnStackMarker,
      final Universe universe) {
    this.bytecodes = bytecodes;
    this.locals = locals;
    this.literalsAndConstants = literals;
    this.maxStackDepth = maxStackDepth;
    this.universe = universe;
    this.indirectCallNode = Truffle.getRuntime().createIndirectCallNode();

    this.stackVar = stackVar;
    this.stackPointer = stackPointer;
    this.frameOnStackMarker = frameOnStackMarker;
  }

  @ExplodeLoop
  private MaterializedFrame determineOuterContext(final VirtualFrame frame) {
    // TODO: change bytecode format to include the context level
    SBlock self = Frame.getSelfBlock(frame);

    MaterializedFrame outer = self.getContext();

    while (true) {
      Object rcvr = Frame.getArgument(outer, 0);

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
    SBlock self = Frame.getSelfBlock(frame);
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
  public Object executeGeneric(final VirtualFrame frame) {
    Object[] stack = new Object[maxStackDepth];
    // TODO: verify this is not needed (shouldn't be by correct stack semantics,
    // which I believe we obey)
    // Arrays.fill(stack, Nil.nilObject);
    frame.setObject(stackVar, stack);
    frame.setInt(stackPointer, -1);

    int bytecodeIndex = 0;

    while (true) {
      byte bytecode = bytecodes[bytecodeIndex];
      int bytecodeLength = getBytecodeLength(bytecode);

      CompilerAsserts.partialEvaluationConstant(bytecodeIndex);
      CompilerAsserts.partialEvaluationConstant(bytecode);

      switch (bytecode) {
        case HALT: {
          return Frame.getStackElement(frame, 0, stackPointer, stackVar);
        }

        case DUP: {
          Frame.duplicateTopOfStack(frame, stackPointer, stackVar);
          break;
        }

        case PUSH_LOCAL: {
          byte localIdx = bytecodes[bytecodeIndex + 1];
          byte contextIdx = bytecodes[bytecodeIndex + 2];

          VirtualFrame currentOrContext = frame;
          if (contextIdx > 0) {
            currentOrContext = determineContext(currentOrContext, contextIdx);
          }
          FrameSlot slot = locals[localIdx];
          Frame.push(frame, currentOrContext.getValue(slot), stackPointer, stackVar);
          break;
        }

        case PUSH_ARGUMENT: {
          byte argIdx = bytecodes[bytecodeIndex + 1];
          byte contextIdx = bytecodes[bytecodeIndex + 2];

          VirtualFrame currentOrContext = frame;
          if (contextIdx > 0) {
            currentOrContext = determineContext(currentOrContext, contextIdx);
          }
          Frame.push(frame, Frame.getArgument(currentOrContext, argIdx), stackPointer,
              stackVar);
          break;
        }

        case PUSH_FIELD: {
          byte fieldIdx = bytecodes[bytecodeIndex + 1];
          Frame.push(frame, Frame.getSelf(frame).getField(fieldIdx), stackPointer, stackVar);
          break;
        }

        case PUSH_BLOCK: {
          byte literalIdx = bytecodes[bytecodeIndex + 1];
          SMethod blockMethod = (SMethod) literalsAndConstants[literalIdx];
          Frame.push(frame,
              new SBlock(blockMethod,
                  universe.getBlockClass(blockMethod.getNumberOfArguments()),
                  frame.materialize()),
              stackPointer, stackVar);
          break;
        }

        case PUSH_CONSTANT: {
          byte literalIdx = bytecodes[bytecodeIndex + 1];
          Frame.push(frame, literalsAndConstants[literalIdx], stackPointer, stackVar);
          break;
        }

        case PUSH_GLOBAL: {
          byte literalIdx = bytecodes[bytecodeIndex + 1];
          SSymbol globalName = (SSymbol) literalsAndConstants[literalIdx];

          Object global = universe.getGlobal(globalName);

          if (global != null) {
            Frame.push(frame, global, stackPointer, stackVar);
          } else {
            // Send 'unknownGlobal:' to self
            SAbstractObject.sendUnknownGlobal(Frame.getSelf(frame), globalName, universe);
          }
          break;
        }

        case POP: {
          Frame.pop(frame, stackPointer);
          break;
        }

        case POP_LOCAL: {
          byte localIdx = bytecodes[bytecodeIndex + 1];
          byte contextIdx = bytecodes[bytecodeIndex + 2];

          VirtualFrame currentOrContext = frame;
          if (contextIdx > 0) {
            currentOrContext = determineContext(currentOrContext, contextIdx);
          }

          FrameSlot slot = locals[localIdx];
          currentOrContext.setObject(slot, Frame.popValue(frame, stackPointer, stackVar));
          break;
        }

        case POP_ARGUMENT: {
          byte argIdx = bytecodes[bytecodeIndex + 1];
          byte contextIdx = bytecodes[bytecodeIndex + 2];

          VirtualFrame currentOrContext = frame;
          if (contextIdx > 0) {
            currentOrContext = determineContext(currentOrContext, contextIdx);
          }

          Frame.setArgument(currentOrContext, argIdx,
              Frame.popValue(frame, stackPointer, stackVar));
          break;
        }

        case POP_FIELD: {
          byte fieldIdx = bytecodes[bytecodeIndex + 1];
          Frame.getSelf(frame).setField(
              fieldIdx,
              Frame.popValue(frame, stackPointer, stackVar));
          break;
        }

        case SEND: {
          doSend(frame, bytecodeIndex);
          break;
        }

        case SUPER_SEND: {
          doSuperSend(frame, bytecodeIndex);
          break;
        }

        case RETURN_LOCAL: {
          return Frame.popValue(frame, stackPointer, stackVar);
        }

        case RETURN_NON_LOCAL: {
          doReturnNonLocal(frame);
          break;
        }

        default:
          Universe.errorPrintln("Nasty bug in interpreter");
          break;
      }

      bytecodeIndex = bytecodeIndex + bytecodeLength;
    }
  }

  private SClass getHolder() {
    return ((Invokable) getRootNode()).getHolder();
  }

  private void doSuperSend(final VirtualFrame frame, final int bytecodeIndex) {
    byte literalIdx = bytecodes[bytecodeIndex + 1];
    SSymbol signature = (SSymbol) literalsAndConstants[literalIdx];

    SClass holderSuper = (SClass) getHolder().getSuperClass();
    SInvokable invokable = holderSuper.lookupInvokable(signature);

    int numberOfArguments = signature.getNumberOfSignatureArguments();
    Object[] callArgs =
        Frame.popCallArguments(frame, numberOfArguments, stackPointer, stackVar);
    performInvoke(frame, signature, invokable, callArgs);
  }

  private void performInvoke(final VirtualFrame frame, final SSymbol signature,
      final SInvokable invokable, final Object[] callArgs) {
    Object result;
    if (invokable != null) {
      result = invokable.invoke(indirectCallNode, callArgs);
    } else {
      SArray argumentsArray = SArguments.getArgumentsWithoutReceiver(callArgs);
      CallTarget callTarget = CachedDnuNode.getDnuCallTarget(getHolder(), universe);
      result = indirectCallNode.call(callTarget, callArgs[0], signature, argumentsArray);
    }
    Frame.push(frame, result, stackPointer, stackVar);
  }

  private void doReturnNonLocal(final VirtualFrame frame) {
    Object result = Frame.popValue(frame, stackPointer, stackVar);

    MaterializedFrame ctx = determineOuterContext(frame);
    FrameOnStackMarker marker =
        (FrameOnStackMarker) FrameUtil.getObjectSafe(ctx, frameOnStackMarker);

    if (marker.isOnStack()) {
      throw new ReturnException(result, marker);
    } else {
      SBlock block = (SBlock) SArguments.rcvr(frame);
      Object self = SArguments.rcvr(ctx);
      Object ebResult = SAbstractObject.sendEscapedBlock(self, block, universe);
      Frame.push(frame, ebResult, stackPointer, stackVar);
    }
  }

  private void doSend(final VirtualFrame frame, final int bytecodeIndex) {
    byte literalIdx = bytecodes[bytecodeIndex + 1];
    SSymbol signature = (SSymbol) literalsAndConstants[literalIdx];

    int numberOfArguments = signature.getNumberOfSignatureArguments();
    Object[] callArgs =
        Frame.popCallArguments(frame, numberOfArguments, stackPointer, stackVar);

    SClass rcvrClass = Types.getClassOf(callArgs[0], universe);
    SInvokable invokable = rcvrClass.lookupInvokable(signature);
    performInvoke(frame, signature, invokable, callArgs);
  }

  public int getNumberOfLocals() {
    return locals.length;
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
