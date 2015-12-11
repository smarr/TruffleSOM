package som.interpreter.nodes.dispatch;

import som.interpreter.SArguments;
import som.interpreter.nodes.dispatch.AbstractMethodDispatchNode.AbstractMethodCachedDispatchNode;
import som.vm.constants.ExecutionLevel;
import som.vmobjects.SInvokable;
import som.vmobjects.SInvokable.SMethod;
import som.vmobjects.SMateEnvironment;

import com.oracle.truffle.api.frame.VirtualFrame;


public final class CachedMethodDispatchNode extends AbstractMethodCachedDispatchNode {

  private final SInvokable cachedSomMethod;

  public CachedMethodDispatchNode(final SInvokable method,
      final AbstractMethodDispatchNode nextInCache) {
    super(method.getCallTarget(), nextInCache);
    this.cachedSomMethod = method;
  }

  @Override
  public Object executeDispatch(final VirtualFrame frame, final SMateEnvironment environment, final ExecutionLevel exLevel, final SMethod method, final Object[] arguments) {
    if (method == cachedSomMethod) {
      return cachedMethod.call(frame, SArguments.createSArguments(environment, exLevel, arguments));
    } else {
      return nextInCache.executeDispatch(frame, environment, exLevel, method, arguments);
    }
  }
}
