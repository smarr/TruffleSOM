package trufflesom.interpreter.supernodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.interpreter.nodes.AbstractMessageSendNode;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.vmobjects.SSymbol;


public class QuatArgSendNode extends AbstractMessageSendNode {
  private final int                   argIdx;
  private final SSymbol               selector;
  @Child private AbstractDispatchNode dispatchNode;
  @Child private ExpressionNode       arg1;
  @Child private ExpressionNode       arg2;
  @Child private ExpressionNode       arg3;

  public QuatArgSendNode(final int argIdx, final ExpressionNode arg1,
      final ExpressionNode arg2,
      final ExpressionNode arg3, final SSymbol selector,
      final AbstractDispatchNode dispatchNode) {
    super(4, null);
    this.argIdx = argIdx;
    this.selector = selector;
    this.dispatchNode = dispatchNode;
    this.arg1 = arg1;
    this.arg2 = arg2;
    this.arg3 = arg3;
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    Object[] arguments = new Object[] {
        frame.getArguments()[argIdx],
        arg1.executeGeneric(frame),
        arg2.executeGeneric(frame),
        arg3.executeGeneric(frame)};
    return doPreEvaluated(frame, arguments);
  }

  @Override
  public Object doPreEvaluated(final VirtualFrame frame,
      final Object[] arguments) {
    return dispatchNode.executeDispatch(frame, arguments);
  }

  @Override
  public SSymbol getInvocationIdentifier() {
    return selector;
  }

  @Override
  public void notifyDispatchInserted() {
    dispatchNode.notifyAsInserted();
  }
}
