package trufflesom.interpreter.nodes.dispatch;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;


public abstract class AbstractDispatchNode extends Node implements DispatchChain {
  public static final int INLINE_CACHE_SIZE = 6;

  public abstract Object executeDispatch(VirtualFrame frame, Object[] arguments);

  public abstract Object executeUnary(VirtualFrame frame, Object rcvr);

  public abstract Object executeBinary(VirtualFrame frame, Object rcvr, Object arg);

  public abstract Object executeTernary(VirtualFrame frame, Object rcvr, Object arg1,
      Object arg2);

  public abstract Object executeQuat(VirtualFrame frame, Object rcvr, Object arg1, Object arg2,
      Object arg3);

  public abstract static class AbstractCachedDispatchNode
      extends AbstractDispatchNode {

    @Child protected DirectCallNode       cachedMethod;
    @Child protected AbstractDispatchNode nextInCache;

    public AbstractCachedDispatchNode(final CallTarget methodCallTarget,
        final AbstractDispatchNode nextInCache) {
      DirectCallNode cachedMethod =
          Truffle.getRuntime().createDirectCallNode(methodCallTarget);

      this.cachedMethod = cachedMethod;
      this.nextInCache = nextInCache;
    }

    @Override
    public final int lengthOfDispatchChain() {
      return 1 + nextInCache.lengthOfDispatchChain();
    }
  }
}
