package trufflesom.interpreter.nodes.dispatch;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;

import trufflesom.vmobjects.SBlock;


public final class GenericBlockDispatchNode extends AbstractDispatchNode {
  @Child private IndirectCallNode call;

  public GenericBlockDispatchNode() {
    call = insert(Truffle.getRuntime().createIndirectCallNode());
  }

  @Override
  public Object doPreEvaluated(final VirtualFrame frame, final Object[] arguments) {
    SBlock rcvr = (SBlock) arguments[0];
    return call.call(rcvr.getMethod().getCallTarget(), arguments);
  }

  @Override
  public boolean entryMatches(final Object rcvr) throws InvalidAssumptionException {
    return true;
  }
}
