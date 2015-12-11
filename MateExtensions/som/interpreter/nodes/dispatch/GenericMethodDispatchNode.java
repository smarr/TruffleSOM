package som.interpreter.nodes.dispatch;

import som.interpreter.SArguments;
import som.vm.constants.ExecutionLevel;
import som.vmobjects.SBlock;
import som.vmobjects.SMateEnvironment;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;

public final class GenericMethodDispatchNode extends AbstractDispatchNode {
  @Child private IndirectCallNode call = Truffle.getRuntime().createIndirectCallNode();

  @Override
  public Object executeDispatch(final VirtualFrame frame, final SMateEnvironment environment, final ExecutionLevel exLevel,
      final Object[] arguments) {
    SBlock rcvr = (SBlock) arguments[0];
    return rcvr.getMethod().invoke(frame, call, SArguments.createSArguments(environment, exLevel, arguments));
  }

  @Override
  public int lengthOfDispatchChain() {
    return 1000;
  }
}