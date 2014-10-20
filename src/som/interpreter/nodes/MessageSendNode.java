package som.interpreter.nodes;

import som.interpreter.nodes.ArgumentReadNode.NonLocalSuperReadNode;
import som.interpreter.nodes.dispatch.AbstractDispatchNode;
import som.interpreter.nodes.dispatch.GenericDispatchNode;
import som.interpreter.nodes.dispatch.SuperDispatchNode;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.source.SourceSection;

public final class MessageSendNode extends ExpressionNode {

  public static MessageSendNode createSuper(final SSymbol selector,
      final ExpressionNode[] arguments, final SourceSection source) {
    return new MessageSendNode(selector, arguments,
        SuperDispatchNode.create(selector, (NonLocalSuperReadNode) arguments[0]),
        source);
  }

  public static MessageSendNode createGeneric(final SSymbol selector,
      final ExpressionNode[] argumentNodes, final SourceSection source) {
    return new MessageSendNode(selector, argumentNodes,
        new GenericDispatchNode(selector), source);
  }

  private final SSymbol selector;
  @Children protected final ExpressionNode[] argumentNodes;
  @Child private AbstractDispatchNode dispatchNode;

  private MessageSendNode(final SSymbol selector,
      final ExpressionNode[] arguments,
      final AbstractDispatchNode dispatchNode, final SourceSection source) {
    super(source);
    this.argumentNodes = arguments;
    this.selector = selector;
    this.dispatchNode = dispatchNode;
    if (arguments[0] instanceof NonLocalSuperReadNode) {
      assert dispatchNode instanceof SuperDispatchNode;
    }
  }

  @Override
  public final Object executeGeneric(final VirtualFrame frame) {
    Object[] arguments = evaluateArguments(frame);
    return dispatchNode.executeDispatch(frame, arguments);
  }

  @ExplodeLoop
  private Object[] evaluateArguments(final VirtualFrame frame) {
    Object[] arguments = new Object[argumentNodes.length];
    for (int i = 0; i < argumentNodes.length; i++) {
      arguments[i] = argumentNodes[i].executeGeneric(frame);
      assert arguments[i] != null;
    }
    return arguments;
  }

  @Override
  public String toString() {
    return "MsgSend(" + selector.getString() + ")";
  }
}
