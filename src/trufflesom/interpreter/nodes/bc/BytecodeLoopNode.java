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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;

import trufflesom.interpreter.SArguments;
import trufflesom.interpreter.bc.Frame;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.dispatch.CachedDnuNode;
import trufflesom.vm.NotYetImplementedException;
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

  @CompilationFinal(dimensions = 1) private final byte[]      bytecodes;
  @CompilationFinal(dimensions = 1) private final FrameSlot[] locals;
  @CompilationFinal(dimensions = 1) private final Object[]    literals;

  private final IndirectCallNode indirectCallNode;

  private final int      maxStackDepth;
  private final Universe universe;

  public BytecodeLoopNode(final byte[] bytecodes, final FrameSlot[] locals,
      final Object[] literals, final int maxStackDepth, final Universe universe) {
    this.bytecodes = bytecodes;
    this.locals = locals;
    this.literals = literals;
    this.maxStackDepth = maxStackDepth;
    this.universe = universe;
    this.indirectCallNode = Truffle.getRuntime().createIndirectCallNode();
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    int bytecodeIndex = 0;

    while (true) {
      byte bytecode = bytecodes[bytecodeIndex];
      int bytecodeLength = getBytecodeLength(bytecode);

      CompilerAsserts.partialEvaluationConstant(bytecodeIndex);
      CompilerAsserts.partialEvaluationConstant(bytecode);

      switch (bytecode) {
        case HALT: {
          return Frame.getStackElement(frame, 0);
        }

        case DUP: {
          Frame.duplicateTopOfStack(frame);
          break;
        }

        case PUSH_LOCAL: {
          byte localIdx = bytecodes[bytecodeIndex + 1];
          byte contextIdx = bytecodes[bytecodeIndex + 2];

          assert contextIdx == 0 : "TODO: walk context chain";
          FrameSlot slot = locals[localIdx];
          Frame.push(frame, frame.getValue(slot));
          break;
        }

        case PUSH_ARGUMENT: {
          byte argIdx = bytecodes[bytecodeIndex + 1];
          byte contextIdx = bytecodes[bytecodeIndex + 2];

          assert contextIdx == 0 : "TODO: walk context chain";
          Frame.push(frame, Frame.getArgument(frame, argIdx));
          break;
        }

        case PUSH_FIELD: {
          byte fieldIdx = bytecodes[bytecodeIndex + 1];
          Frame.push(frame, Frame.getSelf(frame).getField(fieldIdx));
          break;
        }

        case PUSH_BLOCK: {
          byte literalIdx = bytecodes[bytecodeIndex + 1];
          SMethod blockMethod = (SMethod) literals[literalIdx];
          Frame.push(frame,
              new SBlock(blockMethod,
                  universe.getBlockClass(blockMethod.getNumberOfArguments()),
                  frame.materialize()));
          break;
        }

        case PUSH_CONSTANT: {
          byte literalIdx = bytecodes[bytecodeIndex + 1];
          Frame.push(frame, literals[literalIdx]);
          break;
        }

        case PUSH_GLOBAL: {
          byte literalIdx = bytecodes[bytecodeIndex + 1];
          SSymbol globalName = (SSymbol) literals[literalIdx];

          Object global = universe.getGlobal(globalName);

          if (global != null) {
            Frame.push(frame, global);
          } else {
            // Send 'unknownGlobal:' to self
            SAbstractObject.sendUnknownGlobal(Frame.getSelf(frame), globalName, universe);
          }
          break;
        }

        case POP: {
          Frame.pop(frame);
          break;
        }

        case POP_LOCAL: {
          byte localIdx = bytecodes[bytecodeIndex + 1];
          byte contextIdx = bytecodes[bytecodeIndex + 2];
          assert contextIdx == 0 : "TODO: walk context chain";

          FrameSlot slot = locals[localIdx];
          frame.setObject(slot, Frame.popValue(frame));
          break;
        }

        case POP_ARGUMENT: {
          byte argIdx = bytecodes[bytecodeIndex + 1];
          byte contextIdx = bytecodes[bytecodeIndex + 2];
          assert contextIdx == 0 : "TODO: walk context chain";

          Frame.setArgument(frame, argIdx, Frame.popValue(frame));
          break;
        }

        case POP_FIELD: {
          byte fieldIdx = bytecodes[bytecodeIndex + 1];
          Frame.getSelf(frame).setField(fieldIdx, Frame.popValue(frame));
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
          return Frame.popValue(frame);
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
    throw new NotYetImplementedException();
  }

  private void doSuperSend(final VirtualFrame frame, final int bytecodeIndex) {
    byte literalIdx = bytecodes[bytecodeIndex + 1];
    SSymbol signature = (SSymbol) literals[literalIdx];

    SClass holderSuper = (SClass) getHolder().getSuperClass();
    SInvokable invokable = holderSuper.lookupInvokable(signature);

    int numberOfArguments = signature.getNumberOfSignatureArguments();
    Object[] callArgs = Frame.getCallArguments(frame, numberOfArguments);
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
    Frame.push(frame, result);
  }

  private void doReturnNonLocal(final VirtualFrame frame) {
    Object result = Frame.popValue(frame);

    throw new NotYetImplementedException();
  }

  private void doSend(final VirtualFrame frame, final int bytecodeIndex) {
    byte literalIdx = bytecodes[bytecodeIndex + 1];
    SSymbol signature = (SSymbol) literals[literalIdx];

    int numberOfArguments = signature.getNumberOfSignatureArguments();
    Object[] callArgs = Frame.getCallArguments(frame, numberOfArguments);

    SClass rcvrClass = ((SObject) callArgs[0]).getSOMClass(universe);
    SInvokable invokable = rcvrClass.lookupInvokable(signature);
    performInvoke(frame, signature, invokable, callArgs);
  }

}
