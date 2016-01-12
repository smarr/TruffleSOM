package som.interpreter.nodes.dispatch;

import som.vm.constants.ExecutionLevel;
import som.vmobjects.SInvokable;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;

public abstract class AbstractMethodDispatchNode extends Node implements DispatchChain {
  public static final int INLINE_CACHE_SIZE = 6;

  public abstract Object executeDispatch(
      final VirtualFrame frame, final DynamicObject environment, final ExecutionLevel exLevel, final SInvokable method, final Object[] arguments);
  
  public abstract static class AbstractMethodCachedDispatchNode
    extends AbstractMethodDispatchNode {
    @Child protected DirectCallNode       cachedMethod;
    @Child protected AbstractMethodDispatchNode nextInCache;

    public AbstractMethodCachedDispatchNode(final CallTarget methodCallTarget,
        final AbstractMethodDispatchNode nextInCache) {
      DirectCallNode cachedMethod = Truffle.getRuntime().createDirectCallNode(methodCallTarget);

      this.cachedMethod = cachedMethod;
      this.nextInCache  = nextInCache;
    }

    @Override
    public final int lengthOfDispatchChain() {
      return 1 + nextInCache.lengthOfDispatchChain();
    }
  }
}
