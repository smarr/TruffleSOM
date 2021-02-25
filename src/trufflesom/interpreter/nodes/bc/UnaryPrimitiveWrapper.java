package trufflesom.interpreter.nodes.bc;

import static trufflesom.interpreter.bc.Bytecodes.Q_SEND;

import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.interpreter.TruffleCompiler;
import trufflesom.interpreter.nodes.MessageSendNode;
import trufflesom.interpreter.nodes.MessageSendNode.GenericMessageSendNode;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SSymbol;


public class UnaryPrimitiveWrapper extends UnaryExpressionNode {
  @Child private UnaryExpressionNode primitive;

  private final int bytecodeIndex;

  private final Universe universe;
  private final SSymbol  selector;

  public UnaryPrimitiveWrapper(final int bytecodeIndex, final SSymbol signature,
      final UnaryExpressionNode primitive, final Universe universe,
      final SourceSection sourceSection) {
    this.primitive = primitive;
    this.bytecodeIndex = bytecodeIndex;
    this.universe = universe;
    this.sourceSection = sourceSection;
    this.selector = signature;
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    throw new UnsupportedOperationException(
        "In the bytecode interpreter, `executeEvaluated` is expected to be used");
  }

  @Override
  public Object executeEvaluated(final VirtualFrame frame, final Object receiver) {
    try {
      return primitive.executeEvaluated(frame, receiver);
    } catch (UnsupportedSpecializationException e) {
      TruffleCompiler.transferToInterpreterAndInvalidate(
          "Ternary primitive with unsupported specialization.");
      return makeGenericSend().doPreEvaluated(frame,
          new Object[] {receiver});
    }
  }

  private GenericMessageSendNode makeGenericSend() {
    GenericMessageSendNode node =
        MessageSendNode.createGeneric(selector, null, sourceSection, universe);

    assert getParent() instanceof BytecodeLoopNode : "UnaryPrimitiveWrapper are expected to be direct children of a `BytecodeLoopNode`.";
    ((BytecodeLoopNode) getParent()).requicken(bytecodeIndex, Q_SEND);

    return replace(node);
  }
}
