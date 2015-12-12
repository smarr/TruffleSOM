package som.interpreter.nodes.dispatch;

import som.interpreter.SArguments;
import som.vm.constants.ExecutionLevel;
import som.vmobjects.SInvokable.SMethod;
import som.vmobjects.SMateEnvironment;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;

public final class GenericMethodDispatchNode extends AbstractMethodDispatchNode {
  @Child private IndirectCallNode call = Truffle.getRuntime().createIndirectCallNode();

  @Override
  public Object executeDispatch(final VirtualFrame frame, final SMateEnvironment environment, final ExecutionLevel exLevel,
      SMethod method, final Object[] arguments) {
    return call.call(frame, method.getCallTarget(), SArguments.createSArguments(SArguments.getEnvironment(frame), ExecutionLevel.Base, arguments));
  }

  @Override
  public int lengthOfDispatchChain() {
    return 1000;
  }
}