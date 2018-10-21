package som.interpreter.nodes.nary;

import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import som.interpreter.TruffleCompiler;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.MessageSendNode;
import som.interpreter.nodes.MessageSendNode.GenericMessageSendNode;
import som.vm.Universe;
import som.vmobjects.SSymbol;


public class EagerUnaryPrimitiveNode extends EagerPrimitive {

  @Child private ExpressionNode      receiver;
  @Child private UnaryExpressionNode primitive;

  private final SSymbol  selector;
  private final Universe universe;

  public EagerUnaryPrimitiveNode(final SourceSection source, final SSymbol selector,
      final ExpressionNode receiver, final UnaryExpressionNode primitive,
      final Universe universe) {
    super(source);
    this.receiver = insert(receiver);
    this.primitive = insert(primitive);
    this.selector = selector;
    this.universe = universe;
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    Object rcvr = receiver.executeGeneric(frame);

    return executeEvaluated(frame, rcvr);
  }

  public Object executeEvaluated(final VirtualFrame frame,
      final Object receiver) {
    try {
      return primitive.executeEvaluated(frame, receiver);
    } catch (UnsupportedSpecializationException e) {
      TruffleCompiler.transferToInterpreterAndInvalidate(
          "Eager Primitive with unsupported specialization.");
      return makeGenericSend().doPreEvaluated(frame, new Object[] {receiver});
    }
  }

  @Override
  public Object doPreEvaluated(final VirtualFrame frame,
      final Object[] arguments) {
    return executeEvaluated(frame, arguments[0]);
  }

  private GenericMessageSendNode makeGenericSend() {
    GenericMessageSendNode node = MessageSendNode.createGeneric(selector,
        new ExpressionNode[] {receiver}, sourceSection, universe);
    return replace(node);
  }
}
