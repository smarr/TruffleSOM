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


public final class EagerBinaryPrimitiveNode extends EagerPrimitive {

  @Child private ExpressionNode       receiver;
  @Child private ExpressionNode       argument;
  @Child private BinaryExpressionNode primitive;

  private final Universe universe;
  private final SSymbol  selector;

  public EagerBinaryPrimitiveNode(final SourceSection source, final SSymbol selector,
      final ExpressionNode receiver, final ExpressionNode argument,
      final BinaryExpressionNode primitive, final Universe universe) {
    super(source);
    this.receiver = receiver;
    this.argument = argument;
    this.primitive = primitive;
    this.selector = selector;
    this.universe = universe;
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    Object rcvr = receiver.executeGeneric(frame);
    Object arg = argument.executeGeneric(frame);

    return executeEvaluated(frame, rcvr, arg);
  }

  public Object executeEvaluated(final VirtualFrame frame,
      final Object receiver, final Object argument) {
    try {
      return primitive.executeEvaluated(frame, receiver, argument);
    } catch (UnsupportedSpecializationException e) {
      TruffleCompiler.transferToInterpreterAndInvalidate(
          "Eager Primitive with unsupported specialization.");
      return makeGenericSend().doPreEvaluated(frame,
          new Object[] {receiver, argument});
    }
  }

  @Override
  public Object doPreEvaluated(final VirtualFrame frame,
      final Object[] arguments) {
    return executeEvaluated(frame, arguments[0], arguments[1]);
  }

  private GenericMessageSendNode makeGenericSend() {
    GenericMessageSendNode node = MessageSendNode.createGeneric(selector,
        new ExpressionNode[] {receiver, argument}, sourceSection, universe);
    return replace(node);
  }
}
