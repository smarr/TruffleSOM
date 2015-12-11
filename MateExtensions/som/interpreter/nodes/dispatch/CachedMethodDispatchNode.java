package som.interpreter.nodes.dispatch;

import som.interpreter.SArguments;
import som.interpreter.nodes.dispatch.AbstractDispatchNode.AbstractCachedDispatchNode;
import som.vm.constants.ExecutionLevel;
import som.vmobjects.SBlock;
import som.vmobjects.SInvokable;
import som.vmobjects.SMateEnvironment;

import com.oracle.truffle.api.frame.VirtualFrame;


public final class CachedMethodDispatchNode extends AbstractCachedDispatchNode {

  private final SInvokable cachedSomMethod;

  public CachedMethodDispatchNode(final SInvokable method,
      final AbstractDispatchNode nextInCache) {
    super(method.getCallTarget(), nextInCache);
    this.cachedSomMethod = method;
  }

  @Override
  public Object executeDispatch(final VirtualFrame frame, final SMateEnvironment environment, final ExecutionLevel exLevel, final Object[] arguments) {
    SBlock rcvr = (SBlock) arguments[0];
    if (rcvr.getMethod() == cachedSomMethod) {
      return cachedMethod.call(frame, SArguments.createSArguments(environment, exLevel, arguments));
    } else {
      return nextInCache.executeDispatch(frame, environment, exLevel, arguments);
    }
  }
}
