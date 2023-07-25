package trufflesom.interpreter.nodes.dispatch;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.nodes.DirectCallNode;


public abstract class AbstractCachedDispatchNode
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
