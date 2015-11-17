package som.interpreter.nodes.nary;

import som.interpreter.TruffleCompiler;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.MessageSendNode;
import som.interpreter.nodes.MessageSendNode.AbstractMessageSendNode;
import som.interpreter.nodes.MessageSendNode.GenericMessageSendNode;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;


public class EagerQuaternaryPrimitiveNode extends QuaternaryExpressionNode {

  @Child private ExpressionNode receiver;
  @Child private ExpressionNode argument1;
  @Child private ExpressionNode argument2;
  @Child private ExpressionNode argument3;
  @Child private QuaternaryExpressionNode primitive;

  private final SSymbol selector;

  public EagerQuaternaryPrimitiveNode(
      final SSymbol selector,
      final ExpressionNode receiver,
      final ExpressionNode argument1,
      final ExpressionNode argument2,
      final ExpressionNode argument3,
      final QuaternaryExpressionNode primitive) {
    super(null);
    this.receiver  = receiver;
    this.argument1 = argument1;
    this.argument2 = argument2;
    this.argument3 = argument3;
    this.primitive = primitive;
    this.selector = selector;
  }

  public ExpressionNode getReceiver(){return receiver;}
  public ExpressionNode getFirstArg(){return argument1;}
  public ExpressionNode getSecondArg(){return argument2;}
  
  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    Object rcvr = receiver.executeGeneric(frame);
    Object arg1 = argument1.executeGeneric(frame);
    Object arg2 = argument2.executeGeneric(frame);
    Object arg3 = argument3.executeGeneric(frame);

    return executeEvaluated(frame, rcvr, arg1, arg2, arg3);
  }

  @Override
  public Object executeEvaluated(final VirtualFrame frame,
    final Object receiver, final Object argument1, final Object argument2, final Object argument3) {
    try {
      return primitive.executeEvaluated(frame, receiver, argument1, argument2, argument3);
    } catch (UnsupportedSpecializationException e) {
      TruffleCompiler.transferToInterpreterAndInvalidate("Eager Primitive with unsupported specialization.");
      return makeGenericSend().doPreEvaluated(frame,
          new Object[] {receiver, argument1, argument2, argument3});
    }
  }

  private AbstractMessageSendNode makeGenericSend() {
    GenericMessageSendNode node = MessageSendNode.createGeneric(selector,
        new ExpressionNode[] {receiver, argument1, argument2, argument3},
        getSourceSection());
    return replace(node);
  }
  
  protected SSymbol getSelector(){
    return selector;
  }

  @Override
  public ExpressionNode getThirdArg() {
    return argument3;
  }
}
