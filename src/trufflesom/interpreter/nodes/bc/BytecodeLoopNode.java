package trufflesom.interpreter.nodes.bc;

import static trufflesom.compiler.bc.BytecodeGenerator.emit1;
import static trufflesom.compiler.bc.BytecodeGenerator.emit3;
import static trufflesom.compiler.bc.BytecodeGenerator.emitJumpOnBoolWithDummyOffset;
import static trufflesom.compiler.bc.BytecodeGenerator.emitJumpWithDummyOffset;
import static trufflesom.compiler.bc.BytecodeGenerator.emitPOP;
import static trufflesom.compiler.bc.BytecodeGenerator.emitPUSHBLOCK;
import static trufflesom.compiler.bc.BytecodeGenerator.emitPUSHCONSTANT;
import static trufflesom.compiler.bc.BytecodeGenerator.emitPUSHGLOBAL;
import static trufflesom.compiler.bc.BytecodeGenerator.emitRETURNLOCAL;
import static trufflesom.compiler.bc.BytecodeGenerator.emitRETURNNONLOCAL;
import static trufflesom.compiler.bc.BytecodeGenerator.emitSEND;
import static trufflesom.compiler.bc.BytecodeGenerator.emitSUPERSEND;
import static trufflesom.interpreter.bc.Bytecodes.DEC;
import static trufflesom.interpreter.bc.Bytecodes.DUP;
import static trufflesom.interpreter.bc.Bytecodes.HALT;
import static trufflesom.interpreter.bc.Bytecodes.INC;
import static trufflesom.interpreter.bc.Bytecodes.INC_FIELD;
import static trufflesom.interpreter.bc.Bytecodes.INC_FIELD_PUSH;
import static trufflesom.interpreter.bc.Bytecodes.JUMP;
import static trufflesom.interpreter.bc.Bytecodes.JUMP2;
import static trufflesom.interpreter.bc.Bytecodes.JUMP2_BACKWARDS;
import static trufflesom.interpreter.bc.Bytecodes.JUMP2_ON_FALSE_POP;
import static trufflesom.interpreter.bc.Bytecodes.JUMP2_ON_FALSE_TOP_NIL;
import static trufflesom.interpreter.bc.Bytecodes.JUMP2_ON_TRUE_POP;
import static trufflesom.interpreter.bc.Bytecodes.JUMP2_ON_TRUE_TOP_NIL;
import static trufflesom.interpreter.bc.Bytecodes.JUMP_BACKWARDS;
import static trufflesom.interpreter.bc.Bytecodes.JUMP_ON_FALSE_POP;
import static trufflesom.interpreter.bc.Bytecodes.JUMP_ON_FALSE_TOP_NIL;
import static trufflesom.interpreter.bc.Bytecodes.JUMP_ON_TRUE_POP;
import static trufflesom.interpreter.bc.Bytecodes.JUMP_ON_TRUE_TOP_NIL;
import static trufflesom.interpreter.bc.Bytecodes.POP;
import static trufflesom.interpreter.bc.Bytecodes.POP_ARGUMENT;
import static trufflesom.interpreter.bc.Bytecodes.POP_FIELD;
import static trufflesom.interpreter.bc.Bytecodes.POP_FIELD_0;
import static trufflesom.interpreter.bc.Bytecodes.POP_FIELD_1;
import static trufflesom.interpreter.bc.Bytecodes.POP_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.POP_LOCAL_0;
import static trufflesom.interpreter.bc.Bytecodes.POP_LOCAL_1;
import static trufflesom.interpreter.bc.Bytecodes.POP_LOCAL_2;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_ARG1;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_ARG2;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_ARGUMENT;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_BLOCK;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_BLOCK_NO_CTX;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_CONSTANT;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_FIELD;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_FIELD_0;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_FIELD_1;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_GLOBAL;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_LOCAL_0;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_LOCAL_1;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_LOCAL_2;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_SELF;
import static trufflesom.interpreter.bc.Bytecodes.Q_PUSH_GLOBAL;
import static trufflesom.interpreter.bc.Bytecodes.Q_SEND;
import static trufflesom.interpreter.bc.Bytecodes.Q_SEND_1;
import static trufflesom.interpreter.bc.Bytecodes.Q_SEND_2;
import static trufflesom.interpreter.bc.Bytecodes.Q_SEND_3;
import static trufflesom.interpreter.bc.Bytecodes.RETURN_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.RETURN_NON_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.RETURN_SELF;
import static trufflesom.interpreter.bc.Bytecodes.SEND;
import static trufflesom.interpreter.bc.Bytecodes.SUPER_SEND;
import static trufflesom.interpreter.bc.Bytecodes.getBytecodeLength;
import static trufflesom.interpreter.bc.Bytecodes.getBytecodeName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.ExplodeLoop.LoopExplosionKind;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.ValueProfile;

import bd.inlining.ScopeAdaptationVisitor;
import bd.inlining.ScopeAdaptationVisitor.ScopeElement;
import bd.inlining.nodes.ScopeReference;
import bd.primitives.Specializer;
import bd.primitives.nodes.PreevaluatedExpression;
import trufflesom.compiler.Parser.ParseError;
import trufflesom.compiler.Variable.Local;
import trufflesom.compiler.bc.BytecodeMethodGenContext;
import trufflesom.interpreter.EscapedBlockException;
import trufflesom.interpreter.FrameOnStackMarker;
import trufflesom.interpreter.Invokable;
import trufflesom.interpreter.Method;
import trufflesom.interpreter.ReturnException;
import trufflesom.interpreter.SArguments;
import trufflesom.interpreter.Types;
import trufflesom.interpreter.bc.Bytecodes;
import trufflesom.interpreter.bc.RestartLoopException;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.GlobalNode;
import trufflesom.interpreter.nodes.MessageSendNode;
import trufflesom.interpreter.nodes.MessageSendNode.AbstractMessageSendNode;
import trufflesom.interpreter.nodes.MessageSendNode.GenericMessageSendNode;
import trufflesom.interpreter.nodes.literals.IntegerLiteralNode;
import trufflesom.interpreter.nodes.literals.LiteralNode;
import trufflesom.interpreter.nodes.nary.BinaryExpressionNode;
import trufflesom.interpreter.nodes.nary.TernaryExpressionNode;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.IncrementLongFieldNode;
import trufflesom.primitives.Primitives;
import trufflesom.vm.NotYetImplementedException;
import trufflesom.vm.Universe;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SAbstractObject;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


public class BytecodeLoopNode extends ExpressionNode implements ScopeReference {
  private static final ValueProfile frameType = ValueProfile.createClassProfile();
  private static final LiteralNode  dummyNode = new IntegerLiteralNode(0);

  @CompilationFinal(dimensions = 1) private final byte[]      bytecodes;
  @CompilationFinal(dimensions = 1) private final FrameSlot[] localsAndOuters;
  @CompilationFinal(dimensions = 1) private final Object[]    literalsAndConstants;

  @Children private Node[] quickened;

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

    this.frameOnStackMarker = frameOnStackMarker;
  }

  @Override
  public Node deepCopy() {
    return new BytecodeLoopNode(
        bytecodes.clone(), numLocals, localsAndOuters, literalsAndConstants,
        maxStackDepth, frameOnStackMarker, universe).initialize(sourceSection);
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

  public void requicken(final int bytecodeIndex, final byte bytecode, final Node node) {
    bytecodes[bytecodeIndex] = bytecode;
    quickened[bytecodeIndex] = insert(node);
  }

  @Override
  @ExplodeLoop(kind = LoopExplosionKind.MERGE_EXPLODE)
  public Object executeGeneric(final VirtualFrame frame) {
    Object[] stack = new Object[maxStackDepth];
    int stackPointer = -1;
    int bytecodeIndex = 0;

    int backBranchesTaken = 0;

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

        case PUSH_LOCAL_0:
        case PUSH_LOCAL_1:
        case PUSH_LOCAL_2: {
          byte localIdx = (byte) (bytecode - PUSH_LOCAL_0);

          VirtualFrame currentOrContext = frame;
          FrameSlot slot = localsAndOuters[localIdx];

          Object value = currentOrContext.getValue(slot);
          stackPointer += 1;
          stack[stackPointer] = value;
          break;
        }

        case PUSH_ARGUMENT: {
          byte argIdx = bytecodes[bytecodeIndex + 1];
          byte contextIdx = bytecodes[bytecodeIndex + 2];
          assert contextIdx >= 0;

          VirtualFrame currentOrContext = frame;
          if (contextIdx > 0) {
            currentOrContext = determineContext(currentOrContext, contextIdx);
          }

          Object value = currentOrContext.getArguments()[argIdx];
          stackPointer += 1;
          stack[stackPointer] = value;
          break;
        }

        case PUSH_SELF:
        case PUSH_ARG1:
        case PUSH_ARG2: {
          byte argIdx = (byte) (bytecode - PUSH_SELF);
          VirtualFrame currentOrContext = frame;
          Object value = currentOrContext.getArguments()[argIdx];
          stackPointer += 1;
          stack[stackPointer] = value;
          break;
        }

        case PUSH_FIELD:
        case PUSH_FIELD_0:
        case PUSH_FIELD_1: {
          byte fieldIdx;
          byte contextIdx;
          if (bytecode == PUSH_FIELD_0) {
            fieldIdx = 0;
            contextIdx = 0;
          } else if (bytecode == PUSH_FIELD_1) {
            fieldIdx = 1;
            contextIdx = 0;
          } else {
            fieldIdx = bytecodes[bytecodeIndex + 1];
            contextIdx = bytecodes[bytecodeIndex + 2];
          }

          VirtualFrame currentOrContext = frame;
          if (contextIdx > 0) {
            currentOrContext = determineContext(currentOrContext, contextIdx);
          }

          SObject obj = (SObject) currentOrContext.getArguments()[0];

          if (this.quickened == null) {
            this.quickened = new Node[bytecodes.length];
          }
          Node node = quickened[bytecodeIndex];
          if (node == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            node = quickened[bytecodeIndex] = insert(FieldAccessorNode.createRead(fieldIdx));
          }

          Object value = ((AbstractReadFieldNode) node).read(obj);
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

        case PUSH_BLOCK_NO_CTX: {
          byte literalIdx = bytecodes[bytecodeIndex + 1];
          SMethod blockMethod = (SMethod) literalsAndConstants[literalIdx];

          Object value = new SBlock(blockMethod,
              universe.getBlockClass(blockMethod.getNumberOfArguments()), null);
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

          GlobalNode quickened =
              GlobalNode.create(globalName, universe, null).initialize(sourceSection);
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

        case POP_LOCAL_0:
        case POP_LOCAL_1:
        case POP_LOCAL_2: {
          byte localIdx = (byte) (bytecode - POP_LOCAL_0);

          VirtualFrame currentOrContext = frame;
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

        case POP_FIELD:
        case POP_FIELD_0:
        case POP_FIELD_1: {
          byte fieldIdx;
          byte contextIdx;
          if (bytecode == POP_FIELD_0) {
            fieldIdx = 0;
            contextIdx = 0;
          } else if (bytecode == POP_FIELD_1) {
            fieldIdx = 1;
            contextIdx = 0;
          } else {
            fieldIdx = bytecodes[bytecodeIndex + 1];
            contextIdx = bytecodes[bytecodeIndex + 2];
          }

          VirtualFrame currentOrContext = frame;
          if (contextIdx > 0) {
            currentOrContext = determineContext(currentOrContext, contextIdx);
          }

          Object value = stack[stackPointer];
          stackPointer -= 1;

          SObject obj = (SObject) currentOrContext.getArguments()[0];

          if (this.quickened == null) {
            this.quickened = new Node[bytecodes.length];
          }
          Node node = quickened[bytecodeIndex];
          if (node == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            quickened[bytecodeIndex] = node = insert(FieldAccessorNode.createWrite(fieldIdx));
          }

          ((AbstractWriteFieldNode) node).write(obj, value);
          break;
        }

        case SEND: {
          CompilerDirectives.transferToInterpreterAndInvalidate();
          try {
            byte literalIdx = bytecodes[bytecodeIndex + 1];
            SSymbol signature = (SSymbol) literalsAndConstants[literalIdx];
            int numberOfArguments = signature.getNumberOfSignatureArguments();

            Object[] callArgs = new Object[numberOfArguments];
            System.arraycopy(stack, stackPointer - numberOfArguments + 1, callArgs, 0,
                numberOfArguments);
            stackPointer -= numberOfArguments;

            Object result = null;
            boolean done = false;

            if (numberOfArguments <= 3) {
              Primitives prims = universe.getPrimitives();
              ExpressionNode[] dummyArgs = new ExpressionNode[numberOfArguments];
              Arrays.fill(dummyArgs, dummyNode);

              Specializer<Universe, ExpressionNode, SSymbol> specializer =
                  prims.getEagerSpecializer(signature, callArgs, dummyArgs);

              if (specializer != null) {
                done = true;
                ExpressionNode quickened = specializer.create(callArgs, dummyArgs,
                    sourceSection, !specializer.noWrapper(), universe);

                if (numberOfArguments == 1) {
                  UnaryExpressionNode q = (UnaryExpressionNode) quickened;
                  if (!specializer.noWrapper()) {
                    quickened = q = new UnaryPrimitiveWrapper(
                        bytecodeIndex, signature, q, universe, sourceSection);
                  }
                  quickenBytecode(bytecodeIndex, Q_SEND_1, quickened);
                  result = q.executeEvaluated(frame, callArgs[0]);
                } else if (numberOfArguments == 2) {
                  BinaryExpressionNode q = (BinaryExpressionNode) quickened;

                  if (!specializer.noWrapper()) {
                    quickened = q = new BinaryPrimitiveWrapper(
                        bytecodeIndex, signature, q, universe, sourceSection);
                  }
                  quickenBytecode(bytecodeIndex, Q_SEND_2, quickened);
                  result = q.executeEvaluated(frame, callArgs[0], callArgs[1]);
                } else if (numberOfArguments == 3) {
                  TernaryExpressionNode q = (TernaryExpressionNode) quickened;

                  if (!specializer.noWrapper()) {
                    quickened = q = new TernaryPrimitiveWrapper(
                        bytecodeIndex, signature, q, universe, sourceSection);
                  }
                  quickenBytecode(bytecodeIndex, Q_SEND_3, quickened);
                  result = q.executeEvaluated(frame, callArgs[0], callArgs[1], callArgs[2]);
                }
              }
            }

            if (!done) {
              GenericMessageSendNode quickened =
                  MessageSendNode.createGeneric(signature, null, sourceSection, universe);
              quickenBytecode(bytecodeIndex, Q_SEND, quickened);

              result = quickened.doPreEvaluated(frame, callArgs);
            }

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
          CompilerDirectives.transferToInterpreterAndInvalidate();
          try {
            byte literalIdx = bytecodes[bytecodeIndex + 1];
            SSymbol signature = (SSymbol) literalsAndConstants[literalIdx];
            int numberOfArguments = signature.getNumberOfSignatureArguments();

            Object[] callArgs = new Object[numberOfArguments];
            System.arraycopy(stack, stackPointer - numberOfArguments + 1, callArgs, 0,
                numberOfArguments);
            stackPointer -= numberOfArguments;

            PreevaluatedExpression quickened = MessageSendNode.createSuperSend(
                (SClass) getHolder().getSuperClass(), signature, null, sourceSection);
            quickenBytecode(bytecodeIndex, Q_SEND, (Node) quickened);

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

        case RETURN_LOCAL: {
          LoopNode.reportLoopCount(this, backBranchesTaken);
          return stack[stackPointer];
        }

        case RETURN_NON_LOCAL: {
          LoopNode.reportLoopCount(this, backBranchesTaken);

          Object result = stack[stackPointer];
          // stackPointer -= 1;
          doReturnNonLocal(frame, bytecodeIndex, result);
          break;
        }

        case RETURN_SELF: {
          LoopNode.reportLoopCount(this, backBranchesTaken);
          return frame.getArguments()[0];
        }

        case INC: {
          Object top = stack[stackPointer];
          if (top instanceof Long) {
            try {
              stack[stackPointer] = Math.addExact((Long) top, 1L);
            } catch (ArithmeticException e) {
              CompilerDirectives.transferToInterpreterAndInvalidate();
              throw new NotYetImplementedException();
            }
          } else {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (top instanceof Double) {
              stack[stackPointer] = ((Double) top) + 1.0d;
            } else {
              throw new NotYetImplementedException();
            }
          }
          break;
        }

        case DEC: {
          Object top = stack[stackPointer];
          if (top instanceof Long) {
            stack[stackPointer] = ((Long) top) - 1;
          } else {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (top instanceof Double) {
              stack[stackPointer] = ((Double) top) - 1.0d;
            } else {
              throw new NotYetImplementedException();
            }
          }
          break;
        }

        case INC_FIELD: {
          byte fieldIdx = bytecodes[bytecodeIndex + 1];
          byte contextIdx = bytecodes[bytecodeIndex + 2];

          VirtualFrame currentOrContext = frame;
          if (contextIdx > 0) {
            currentOrContext = determineContext(currentOrContext, contextIdx);
          }

          SObject obj = (SObject) currentOrContext.getArguments()[0];

          if (this.quickened == null) {
            this.quickened = new Node[bytecodes.length];
          }
          Node node = quickened[bytecodeIndex];
          if (node == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Object val = obj.getField(fieldIdx);
            if (!(val instanceof Long)) {
              throw new NotYetImplementedException();
            }

            try {
              long longVal = Math.addExact((Long) val, 1);
              obj.setField(fieldIdx, longVal);
            } catch (ArithmeticException e) {
              throw new NotYetImplementedException();
            }

            node = quickened[bytecodeIndex] =
                insert(FieldAccessorNode.createIncrement(fieldIdx, obj));
            break;
          }

          ((IncrementLongFieldNode) node).increment(obj);
          break;
        }

        case INC_FIELD_PUSH: {
          byte fieldIdx = bytecodes[bytecodeIndex + 1];
          byte contextIdx = bytecodes[bytecodeIndex + 2];

          VirtualFrame currentOrContext = frame;
          if (contextIdx > 0) {
            currentOrContext = determineContext(currentOrContext, contextIdx);
          }

          SObject obj = (SObject) currentOrContext.getArguments()[0];

          if (this.quickened == null) {
            this.quickened = new Node[bytecodes.length];
          }
          Node node = quickened[bytecodeIndex];
          if (node == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Object val = obj.getField(fieldIdx);
            if (!(val instanceof Long)) {
              throw new NotYetImplementedException();
            }

            try {
              long longVal = Math.addExact((Long) val, 1);
              obj.setField(fieldIdx, longVal);
              stackPointer += 1;
              stack[stackPointer] = longVal;
            } catch (ArithmeticException e) {
              throw new NotYetImplementedException();
            }

            node = quickened[bytecodeIndex] =
                insert(FieldAccessorNode.createIncrement(fieldIdx, obj));
            break;
          }

          long value = ((IncrementLongFieldNode) node).increment(obj);
          stackPointer += 1;
          stack[stackPointer] = value;
          break;
        }

        case JUMP: {
          int offset = Byte.toUnsignedInt(bytecodes[bytecodeIndex + 1]);
          nextBytecodeIndex = bytecodeIndex + offset;
          break;
        }

        case JUMP_ON_TRUE_TOP_NIL: {
          Object val = stack[stackPointer];
          if (val == Boolean.TRUE) {
            int offset = Byte.toUnsignedInt(bytecodes[bytecodeIndex + 1]);
            nextBytecodeIndex = bytecodeIndex + offset;
            stack[stackPointer] = Nil.nilObject;
          } else {
            stackPointer -= 1;
          }
          break;
        }

        case JUMP_ON_FALSE_TOP_NIL: {
          Object val = stack[stackPointer];
          if (val == Boolean.FALSE) {
            int offset = Byte.toUnsignedInt(bytecodes[bytecodeIndex + 1]);
            nextBytecodeIndex = bytecodeIndex + offset;
            stack[stackPointer] = Nil.nilObject;
          } else {
            stackPointer -= 1;
          }
          break;
        }

        case JUMP_ON_TRUE_POP: {
          Object val = stack[stackPointer];
          if (val == Boolean.TRUE) {
            int offset = Byte.toUnsignedInt(bytecodes[bytecodeIndex + 1]);
            nextBytecodeIndex = bytecodeIndex + offset;
          }
          stackPointer -= 1;
          break;
        }

        case JUMP_ON_FALSE_POP: {
          Object val = stack[stackPointer];
          if (val == Boolean.FALSE) {
            int offset = Byte.toUnsignedInt(bytecodes[bytecodeIndex + 1]);
            nextBytecodeIndex = bytecodeIndex + offset;
          }
          stackPointer -= 1;
          break;
        }

        case JUMP_BACKWARDS: {
          int offset = Byte.toUnsignedInt(bytecodes[bytecodeIndex + 1]);
          nextBytecodeIndex = bytecodeIndex - offset;
          break;
        }

        case JUMP2: {
          int offset = Byte.toUnsignedInt(bytecodes[bytecodeIndex + 1])
              + (Byte.toUnsignedInt(bytecodes[bytecodeIndex + 2]) << 8);
          nextBytecodeIndex = bytecodeIndex + offset;

          if (CompilerDirectives.inInterpreter()) {
            backBranchesTaken += 1;
          }
          break;
        }

        case JUMP2_ON_TRUE_TOP_NIL: {
          Object val = stack[stackPointer];
          if (val == Boolean.TRUE) {
            int offset = Byte.toUnsignedInt(bytecodes[bytecodeIndex + 1])
                + (Byte.toUnsignedInt(bytecodes[bytecodeIndex + 2]) << 8);
            nextBytecodeIndex = bytecodeIndex + offset;
            stack[stackPointer] = Nil.nilObject;
          } else {
            stackPointer -= 1;
          }
          break;
        }

        case JUMP2_ON_FALSE_TOP_NIL: {
          Object val = stack[stackPointer];
          if (val == Boolean.FALSE) {
            int offset = Byte.toUnsignedInt(bytecodes[bytecodeIndex + 1])
                + (Byte.toUnsignedInt(bytecodes[bytecodeIndex + 2]) << 8);
            nextBytecodeIndex = bytecodeIndex + offset;
            stack[stackPointer] = Nil.nilObject;
          } else {
            stackPointer -= 1;
          }
          break;
        }

        case JUMP2_ON_TRUE_POP: {
          Object val = stack[stackPointer];
          if (val == Boolean.TRUE) {
            int offset = Byte.toUnsignedInt(bytecodes[bytecodeIndex + 1])
                + (Byte.toUnsignedInt(bytecodes[bytecodeIndex + 2]) << 8);
            nextBytecodeIndex = bytecodeIndex + offset;
          }
          stackPointer -= 1;
          break;
        }

        case JUMP2_ON_FALSE_POP: {
          Object val = stack[stackPointer];
          if (val == Boolean.FALSE) {
            int offset = Byte.toUnsignedInt(bytecodes[bytecodeIndex + 1])
                + (Byte.toUnsignedInt(bytecodes[bytecodeIndex + 2]) << 8);
            nextBytecodeIndex = bytecodeIndex + offset;
          }
          stackPointer -= 1;
          break;
        }

        case JUMP2_BACKWARDS: {
          int offset = Byte.toUnsignedInt(bytecodes[bytecodeIndex + 1])
              + (Byte.toUnsignedInt(bytecodes[bytecodeIndex + 2]) << 8);
          nextBytecodeIndex = bytecodeIndex - offset;

          if (CompilerDirectives.inInterpreter()) {
            backBranchesTaken += 1;
          }
          break;
        }

        case Q_PUSH_GLOBAL: {
          stackPointer += 1;
          stack[stackPointer] = ((GlobalNode) quickened[bytecodeIndex]).executeGeneric(frame);
          break;
        }

        case Q_SEND: {
          AbstractMessageSendNode node = (AbstractMessageSendNode) quickened[bytecodeIndex];

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

        case Q_SEND_1: {
          Object rcvr = stack[stackPointer];

          stackPointer -= 1;

          try {
            assert quickened[bytecodeIndex] instanceof UnaryExpressionNode;
            UnaryExpressionNode node = (UnaryExpressionNode) quickened[bytecodeIndex];
            Object result = node.executeEvaluated(frame, rcvr);

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

        case Q_SEND_2: {
          Object rcvr = stack[stackPointer - 1];
          Object arg = stack[stackPointer];

          stackPointer -= 2;

          try {
            assert quickened[bytecodeIndex] instanceof BinaryExpressionNode;
            BinaryExpressionNode node = (BinaryExpressionNode) quickened[bytecodeIndex];
            Object result = node.executeEvaluated(frame, rcvr, arg);

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

        case Q_SEND_3: {
          Object rcvr = stack[stackPointer - 2];
          Object arg1 = stack[stackPointer - 1];
          Object arg2 = stack[stackPointer];

          stackPointer -= 3;

          try {
            assert quickened[bytecodeIndex] instanceof TernaryExpressionNode;
            TernaryExpressionNode node = (TernaryExpressionNode) quickened[bytecodeIndex];
            Object result = node.executeEvaluated(frame, rcvr, arg1, arg2);

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
          CompilerDirectives.transferToInterpreter();
          throw new NotYetImplementedException("The bytecode " + bytecode + " ("
              + Bytecodes.getBytecodeName(bytecode) + ") is not yet implemented.");
      }

      bytecodeIndex = nextBytecodeIndex;
    }
  }

  private void quickenBytecode(final int bytecodeIndex, final byte quickenedBytecode,
      final Node quickenedNode) {
    if (this.quickened == null) {
      this.quickened = new Node[bytecodes.length];
    }
    this.quickened[bytecodeIndex] = insert(quickenedNode);
    bytecodes[bytecodeIndex] = quickenedBytecode;
  }

  private SClass getHolder() {
    return ((Invokable) getRootNode()).getHolder();
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

  public List<Byte> getBytecodes() {
    List<Byte> list = new ArrayList<>(bytecodes.length);
    for (byte b : bytecodes) {
      list.add(b);
    }
    return list;
  }

  public byte[] getBytecodeArray() {
    return bytecodes;
  }

  public Object getConstant(final int idx) {
    return literalsAndConstants[idx];
  }

  @Override
  public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
    Object scope = inliner.getCurrentScope();
    int targetContextLevel = inliner.contextLevel;

    if (scope instanceof BytecodeMethodGenContext) {
      BytecodeMethodGenContext mgenc = (BytecodeMethodGenContext) scope;

      try {
        inlineInto(mgenc, targetContextLevel);
      } catch (ParseError e) {
        throw new RuntimeException(e);
      }
    } else {
      boolean requiresChangesToContextLevels = inliner.outerScopeChanged();
      adapt(inliner, requiresChangesToContextLevels);
    }
  }

  private static final class Jump implements Comparable<Jump> {
    final byte jumpBc;

    final int originalTarget;
    final int offsetIdx;

    Jump(final byte bc, final int target, final int offsetIdx) {
      this.jumpBc = bc;
      this.originalTarget = target;
      this.offsetIdx = offsetIdx;
      assert target > 0;
    }

    @Override
    public int compareTo(final Jump o) {
      return this.originalTarget - o.originalTarget;
    }

    @Override
    public String toString() {
      return Bytecodes.getBytecodeName(jumpBc) + " -> " + originalTarget;
    }
  }

  private void inlineInto(final BytecodeMethodGenContext mgenc, final int targetContextLevel)
      throws ParseError {
    PriorityQueue<Jump> jumps = new PriorityQueue<>();

    int i = 0;
    while (i < bytecodes.length) {
      while (!jumps.isEmpty() && jumps.element().originalTarget == i) {
        Jump j = jumps.remove();
        mgenc.patchJumpOffsetToPointToNextInstruction(j.offsetIdx, null);
      }

      byte bytecode = bytecodes[i];
      final int bytecodeLength = getBytecodeLength(bytecode);

      switch (bytecode) {
        case HALT:
        case DUP: {
          emit1(mgenc, bytecode, bytecode == HALT ? 0 : 1);
          break;
        }

        case PUSH_LOCAL: {
          byte localIdx = bytecodes[i + 1];
          FrameSlot frameSlot = localsAndOuters[localIdx];
          Local local = (Local) frameSlot.getIdentifier();
          local.emitPush(mgenc);
          break;
        }

        case PUSH_LOCAL_0:
        case PUSH_LOCAL_1:
        case PUSH_LOCAL_2: {
          byte localIdx = (byte) (bytecode - PUSH_LOCAL_0);
          FrameSlot frameSlot = localsAndOuters[localIdx];
          Local local = (Local) frameSlot.getIdentifier();
          local.emitPush(mgenc);
          break;
        }

        case PUSH_ARGUMENT:
        case PUSH_FIELD: {
          byte argIdx = bytecodes[i + 1];
          byte contextIdx = bytecodes[i + 2];
          emit3(mgenc, bytecode, argIdx, (byte) (contextIdx - 1), 1);
          break;
        }

        case PUSH_SELF:
        case PUSH_ARG1:
        case PUSH_ARG2:
        case PUSH_FIELD_0:
        case PUSH_FIELD_1: {
          throw new IllegalStateException("contextLevel is 0, so, not expected to be here");
        }

        case PUSH_BLOCK:
        case PUSH_BLOCK_NO_CTX: {
          byte literalIdx = bytecodes[i + 1];
          SMethod blockMethod = (SMethod) literalsAndConstants[literalIdx];

          Method blockIvk = (Method) blockMethod.getInvokable();
          Method adapted = blockIvk.cloneAndAdaptAfterScopeChange(null,
              mgenc.getCurrentLexicalScope().getScope(blockIvk),
              targetContextLevel + 1, true, true);
          SMethod newMethod = new SMethod(blockMethod.getSignature(), adapted,
              blockMethod.getEmbeddedBlocks(), blockIvk.getSourceSection());
          newMethod.setHolder(blockMethod.getHolder());
          mgenc.addLiteralIfAbsent(newMethod, null);
          emitPUSHBLOCK(mgenc, newMethod, bytecodes[i] == PUSH_BLOCK);
          break;
        }

        case PUSH_CONSTANT: {
          byte literalIdx = bytecodes[i + 1];
          Object value = literalsAndConstants[literalIdx];
          mgenc.addLiteralIfAbsent(value, null);
          emitPUSHCONSTANT(mgenc, value);
          break;
        }

        case PUSH_GLOBAL: {
          byte literalIdx = bytecodes[i + 1];
          SSymbol globalName = (SSymbol) literalsAndConstants[literalIdx];
          mgenc.addLiteralIfAbsent(globalName, null);
          emitPUSHGLOBAL(mgenc, globalName);
          break;
        }

        case POP: {
          emitPOP(mgenc);
          break;
        }

        case POP_LOCAL: {
          byte localIdx = bytecodes[i + 1];
          FrameSlot frameSlot = localsAndOuters[localIdx];
          Local local = (Local) frameSlot.getIdentifier();
          local.emitPop(mgenc);
          break;
        }

        case POP_LOCAL_0:
        case POP_LOCAL_1:
        case POP_LOCAL_2: {
          byte localIdx = (byte) (bytecode - POP_LOCAL_0);
          FrameSlot frameSlot = localsAndOuters[localIdx];
          Local local = (Local) frameSlot.getIdentifier();
          local.emitPop(mgenc);
          break;
        }

        case POP_ARGUMENT:
        case POP_FIELD: {
          byte argIdx = bytecodes[i + 1];
          byte contextIdx = bytecodes[i + 2];
          emit3(mgenc, bytecode, argIdx, (byte) (contextIdx - 1), -1);
          break;
        }

        case POP_FIELD_0:
        case POP_FIELD_1: {
          throw new IllegalStateException("contextLevel is 0, so, not expected to be here");
        }

        case SEND: {
          byte literalIdx = bytecodes[i + 1];
          SSymbol signature = (SSymbol) literalsAndConstants[literalIdx];
          mgenc.addLiteralIfAbsent(signature, null);
          emitSEND(mgenc, signature);
          break;
        }

        case SUPER_SEND: {
          byte literalIdx = bytecodes[i + 1];
          SSymbol signature = (SSymbol) literalsAndConstants[literalIdx];
          mgenc.addLiteralIfAbsent(signature, null);
          emitSUPERSEND(mgenc, signature);
          break;
        }

        case RETURN_LOCAL: {
          // simply don't translate
          assert i == bytecodes.length - 1;
          break;
        }

        case RETURN_NON_LOCAL: {
          byte contextIdx = bytecodes[i + 1];
          byte newCtx = (byte) (contextIdx - 1);
          if (newCtx == 0) {
            emitRETURNLOCAL(mgenc);
          } else {
            emitRETURNNONLOCAL(mgenc);
          }
          break;
        }

        case RETURN_SELF: {
          throw new IllegalStateException(
              "I wouldn't expect RETURN_SELF ever to be inlined, since it's only generated in the most outer methods");
        }

        case INC:
        case DEC: {
          emit1(mgenc, bytecode, 0);
          break;
        }

        case INC_FIELD:
        case INC_FIELD_PUSH: {
          byte fieldIdx = bytecodes[i + 1];
          byte contextIdx = bytecodes[i + 2];
          emit3(mgenc, bytecode, fieldIdx, (byte) (contextIdx - 1),
              bytecode == INC_FIELD ? 0 : 1);
          break;
        }

        case JUMP:
        case JUMP2: {
          byte offset = bytecodes[i + 1];
          int idxOffset = emitJumpWithDummyOffset(mgenc);
          jumps.add(new Jump(JUMP, offset + i, idxOffset));
          break;
        }

        case JUMP_ON_TRUE_TOP_NIL:
        case JUMP2_ON_TRUE_TOP_NIL: {
          byte offset = bytecodes[i + 1];
          int idxOffset = emitJumpOnBoolWithDummyOffset(mgenc, true, false);
          jumps.add(new Jump(JUMP_ON_TRUE_TOP_NIL, offset + i, idxOffset));
          break;
        }

        case JUMP_ON_FALSE_TOP_NIL:
        case JUMP2_ON_FALSE_TOP_NIL: {
          byte offset = bytecodes[i + 1];
          int idxOffset = emitJumpOnBoolWithDummyOffset(mgenc, false, false);
          jumps.add(new Jump(JUMP_ON_FALSE_TOP_NIL, offset + i, idxOffset));
          break;
        }

        case JUMP_ON_TRUE_POP:
        case JUMP2_ON_TRUE_POP: {
          byte offset = bytecodes[i + 1];
          int idxOffset = emitJumpOnBoolWithDummyOffset(mgenc, true, true);
          jumps.add(new Jump(JUMP_ON_TRUE_POP, offset + i, idxOffset));
          break;
        }

        case JUMP_ON_FALSE_POP:
        case JUMP2_ON_FALSE_POP: {
          byte offset = bytecodes[i + 1];
          int idxOffset = emitJumpOnBoolWithDummyOffset(mgenc, false, true);
          jumps.add(new Jump(JUMP_ON_FALSE_POP, offset + i, idxOffset));
          break;
        }

        case JUMP_BACKWARDS:
        case JUMP2_BACKWARDS: {
          byte offset1 = bytecodes[i + 1];
          byte offset2 = bytecodes[i + 2];
          emit3(mgenc, bytecode, offset1, offset2, 0);
          break;
        }

        default:
          throw new NotYetImplementedException(
              "Support for bytecode " + getBytecodeName(bytecode) + " has not yet been added");
      }

      i += bytecodeLength;
    }

    assert jumps.isEmpty();
  }

  private void adapt(final ScopeAdaptationVisitor inliner,
      final boolean requiresChangesToContextLevels) {
    FrameSlot[] oldLocalsAndOuters = Arrays.copyOf(localsAndOuters, localsAndOuters.length);

    int i = 0;
    while (i < bytecodes.length) {
      byte bytecode = bytecodes[i];
      final int bytecodeLength = getBytecodeLength(bytecode);

      switch (bytecode) {
        case HALT:
        case DUP: {
          break;
        }

        case PUSH_LOCAL:
        case PUSH_LOCAL_0:
        case PUSH_LOCAL_1:
        case PUSH_LOCAL_2: {
          byte localIdx;
          if (bytecode == PUSH_LOCAL) {
            localIdx = bytecodes[i + 1];
          } else {
            localIdx = (byte) (bytecode - PUSH_LOCAL_0);
          }
          FrameSlot frameSlot = oldLocalsAndOuters[localIdx];
          Local local = (Local) frameSlot.getIdentifier();
          ScopeElement<ExpressionNode> se = inliner.getAdaptedVar(local);

          if (bytecode == PUSH_LOCAL) {
            bytecodes[i + 2] = (byte) se.contextLevel;
            assert bytecodes[i + 2] >= 0;
          }
          localsAndOuters[localIdx] = ((Local) se.var).getSlot();
          break;
        }

        case PUSH_ARGUMENT: {
          adaptContextIdx(inliner, i, requiresChangesToContextLevels);
          break;
        }

        case PUSH_SELF:
        case PUSH_ARG1:
        case PUSH_ARG2: {
          break;
        }

        case PUSH_FIELD: {
          adaptContextIdx(inliner, i, requiresChangesToContextLevels);
          break;
        }

        case PUSH_FIELD_0:
        case PUSH_FIELD_1: {
          break;
        }

        case PUSH_BLOCK:
        case PUSH_BLOCK_NO_CTX: {
          byte literalIdx = bytecodes[i + 1];
          SMethod blockMethod = (SMethod) literalsAndConstants[literalIdx];

          Method blockIvk = (Method) blockMethod.getInvokable();
          Method adapted =
              blockIvk.cloneAndAdaptAfterScopeChange(null, inliner.getScope(blockIvk),
                  inliner.contextLevel + 1, true, requiresChangesToContextLevels);
          SMethod newMethod = new SMethod(blockMethod.getSignature(), adapted,
              blockMethod.getEmbeddedBlocks(), blockIvk.getSourceSection());
          newMethod.setHolder(blockMethod.getHolder());
          literalsAndConstants[literalIdx] = newMethod;
          break;
        }

        case PUSH_CONSTANT:
        case PUSH_GLOBAL:
        case POP: {
          break;
        }

        case POP_LOCAL:
        case POP_LOCAL_0:
        case POP_LOCAL_1:
        case POP_LOCAL_2: {
          byte localIdx;
          if (bytecode == POP_LOCAL) {
            localIdx = bytecodes[i + 1];
          } else {
            localIdx = (byte) (bytecode - POP_LOCAL_0);
          }
          FrameSlot frameSlot = oldLocalsAndOuters[localIdx];
          Local local = (Local) frameSlot.getIdentifier();
          ScopeElement<ExpressionNode> se = inliner.getAdaptedVar(local);

          if (bytecode == POP_LOCAL) {
            bytecodes[i + 2] = (byte) se.contextLevel;
            assert bytecodes[i + 2] >= 0;
          }
          localsAndOuters[localIdx] = ((Local) se.var).getSlot();
          break;
        }

        case POP_ARGUMENT: {
          adaptContextIdx(inliner, i, requiresChangesToContextLevels);
          break;
        }

        case POP_FIELD: {
          adaptContextIdx(inliner, i, requiresChangesToContextLevels);
          break;
        }

        case POP_FIELD_0:
        case POP_FIELD_1:
        case SEND:
        case SUPER_SEND:
        case RETURN_LOCAL: {
          break;
        }

        case RETURN_NON_LOCAL: {
          byte contextIdx = bytecodes[i + 1];
          if (requiresChangesToContextLevels && contextIdx >= inliner.contextLevel) {
            // we don't simplify to return local, because they had different bytecode length
            // and, well, I don't think this should happen
            assert contextIdx - 1 > 0 : "I wouldn't expect a RETURN_LOCAL equivalent here, "
                + " because we are in a block, or it is already a return local";
            bytecodes[i + 1] = (byte) (contextIdx - 1);
          }
          break;
        }

        case RETURN_SELF:
        case INC:
        case DEC: {
          break;
        }

        case INC_FIELD:
        case INC_FIELD_PUSH: {
          adaptContextIdx(inliner, i, requiresChangesToContextLevels);
          break;
        }

        case JUMP:
        case JUMP_ON_TRUE_TOP_NIL:
        case JUMP_ON_FALSE_TOP_NIL:
        case JUMP_ON_TRUE_POP:
        case JUMP_ON_FALSE_POP:
        case JUMP_BACKWARDS:
        case JUMP2:
        case JUMP2_ON_TRUE_TOP_NIL:
        case JUMP2_ON_FALSE_TOP_NIL:
        case JUMP2_ON_TRUE_POP:
        case JUMP2_ON_FALSE_POP:
        case JUMP2_BACKWARDS: {
          break;
        }

        case Q_PUSH_GLOBAL: {
          bytecodes[i] = PUSH_GLOBAL;
          break;
        }

        case Q_SEND:
        case Q_SEND_1:
        case Q_SEND_2:
        case Q_SEND_3: {
          bytecodes[i] = SEND;
          break;
        }

        default:
          throw new NotYetImplementedException(
              "Support for bytecode " + getBytecodeName(bytecode) + " has not yet been added");
      }

      i += bytecodeLength;
    }
  }

  private void adaptContextIdx(final ScopeAdaptationVisitor inliner, final int i,
      final boolean requiresChangesToContextLevels) {
    if (!requiresChangesToContextLevels) {
      return;
    }

    byte contextIdx = bytecodes[i + 2];
    if (contextIdx >= inliner.contextLevel) {
      byte ctx = (byte) (contextIdx - 1);
      assert ctx >= 0;
      bytecodes[i + 2] = ctx;
    }
  }

  @Override
  public String toString() {
    RootNode root = getRootNode();
    if (root == null) {
      return super.toString();
    }
    return getClass().getSimpleName() + "(" + root.getName() + ")";
  }
}
