package trufflesom.interpreter.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.nodes.NodeCost;

import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.DispatchChain.Cost;
import trufflesom.interpreter.nodes.dispatch.GenericDispatchNode;
import trufflesom.vm.VmSettings;
import trufflesom.vmobjects.SSymbol;


public class GenericMessageSendNode extends AbstractMessageSendNode {

  private final SSymbol selector;

  @Child private AbstractDispatchNode dispatchNode;

  GenericMessageSendNode(final SSymbol selector, final ExpressionNode[] arguments,
      final AbstractDispatchNode dispatchNode) {
    super(selector.getNumberOfSignatureArguments(), arguments);
    this.selector = selector;
    this.dispatchNode = dispatchNode;
  }

  /**
   * Only used for GenericMessageSendNodeWrapper.
   */
  protected GenericMessageSendNode() {
    super(0, null);
    selector = null;
  }

  @Override
  public Object doPreEvaluated(final VirtualFrame frame,
      final Object[] arguments) {
    return dispatchNode.executeDispatch(frame, arguments);
  }

  public void replaceDispatchListHead(
      final GenericDispatchNode replacement) {
    CompilerAsserts.neverPartOfCompilation();
    dispatchNode.replace(replacement);
  }

  @Override
  public String toString() {
    return "GMsgSend(" + selector.getString() + ")";
  }

  @Override
  public NodeCost getCost() {
    return Cost.getCost(dispatchNode);
  }

  @Override
  public SSymbol getInvocationIdentifier() {
    return selector;
  }

  public void notifyDispatchInserted() {
    if (VmSettings.UseInstrumentation) {
      notifyInserted(dispatchNode);
    }
  }

  @Override
  public WrapperNode createWrapper(final ProbeNode probe) {
    return new GenericMessageSendNodeWrapper(this, probe);
  }
}
