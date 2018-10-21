package trufflesom.interpreter.nodes.nary;

import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.interpreter.TruffleCompiler;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.MessageSendNode;
import trufflesom.interpreter.nodes.MessageSendNode.AbstractMessageSendNode;
import trufflesom.interpreter.nodes.MessageSendNode.GenericMessageSendNode;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SSymbol;


public final class EagerTernaryPrimitiveNode extends EagerPrimitive {

  @Child private ExpressionNode        receiver;
  @Child private ExpressionNode        argument1;
  @Child private ExpressionNode        argument2;
  @Child private TernaryExpressionNode primitive;

  private final SSymbol  selector;
  private final Universe universe;

  public EagerTernaryPrimitiveNode(final SourceSection source,
      final SSymbol selector,
      final ExpressionNode receiver,
      final ExpressionNode argument1,
      final ExpressionNode argument2,
      final TernaryExpressionNode primitive, final Universe universe) {
    super(source);
    this.receiver = insert(receiver);
    this.argument1 = insert(argument1);
    this.argument2 = insert(argument2);
    this.primitive = insert(primitive);
    this.selector = selector;
    this.universe = universe;
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    Object rcvr = receiver.executeGeneric(frame);
    Object arg1 = argument1.executeGeneric(frame);
    Object arg2 = argument2.executeGeneric(frame);

    return executeEvaluated(frame, rcvr, arg1, arg2);
  }

  public Object executeEvaluated(final VirtualFrame frame,
      final Object receiver, final Object argument1, final Object argument2) {
    try {
      return primitive.executeEvaluated(frame, receiver, argument1, argument2);
    } catch (UnsupportedSpecializationException e) {
      TruffleCompiler.transferToInterpreterAndInvalidate(
          "Eager Primitive with unsupported specialization.");
      return makeGenericSend().doPreEvaluated(frame,
          new Object[] {receiver, argument1, argument2});
    }
  }

  @Override
  public Object doPreEvaluated(final VirtualFrame frame,
      final Object[] arguments) {
    return executeEvaluated(frame, arguments[0], arguments[1], arguments[2]);
  }

  private AbstractMessageSendNode makeGenericSend() {
    GenericMessageSendNode node = MessageSendNode.createGeneric(selector,
        new ExpressionNode[] {receiver, argument1, argument2},
        sourceSection, universe);
    return replace(node);
  }
}
