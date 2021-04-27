package trufflesom.interpreter.nodes.bc;

import static trufflesom.interpreter.bc.Bytecodes.Q_SEND;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.interpreter.TruffleCompiler;
import trufflesom.interpreter.nodes.MessageSendNode;
import trufflesom.interpreter.nodes.MessageSendNode.GenericMessageSendNode;
import trufflesom.interpreter.nodes.nary.TernaryExpressionNode;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SSymbol;


public class TernaryPrimitiveWrapper extends TernaryExpressionNode {
  @Child private TernaryExpressionNode primitive;

  private final int bytecodeIndex;

  private final Universe universe;
  private final SSymbol  selector;

  public TernaryPrimitiveWrapper(final int bytecodeIndex, final SSymbol signature,
      final TernaryExpressionNode primitive, final Universe universe,
      final SourceSection sourceSection) {
    this.primitive = primitive;
    this.bytecodeIndex = bytecodeIndex;
    this.universe = universe;
    this.selector = signature;
    this.sourceSection = sourceSection;
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    CompilerDirectives.transferToInterpreter();
    throw new UnsupportedOperationException(
        "In the bytecode interpreter, `executeEvaluated` is expected to be used");
  }

  @Override
  public Object executeEvaluated(final VirtualFrame frame,
      final Object receiver, final Object arg1, final Object arg2) {
    try {
      return primitive.executeEvaluated(frame, receiver, arg1, arg2);
    } catch (UnsupportedSpecializationException e) {
      TruffleCompiler.transferToInterpreterAndInvalidate(
          "Ternary primitive with unsupported specialization.");
      return makeGenericSend().doPreEvaluated(frame,
          new Object[] {receiver, arg1, arg2});
    }
  }

  private GenericMessageSendNode makeGenericSend() {
    GenericMessageSendNode node =
        MessageSendNode.createGeneric(selector, null, sourceSection, universe);

    assert getParent() instanceof BytecodeLoopNode : "TernaryPrimitiveWrapper are expected to be direct children of a `BytecodeLoopNode`.";
    ((BytecodeLoopNode) getParent()).requicken(bytecodeIndex, Q_SEND, node);

    return node;
  }
}
