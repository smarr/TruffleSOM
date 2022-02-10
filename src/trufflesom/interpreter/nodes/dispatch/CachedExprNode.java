package trufflesom.interpreter.nodes.dispatch;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.source.Source;

import bd.inlining.nodes.WithSource;
import bd.primitives.nodes.PreevaluatedExpression;
import trufflesom.interpreter.nodes.SOMNode;


public final class CachedExprNode extends AbstractDispatchNode implements WithSource {

  private final DispatchGuard guard;
  private final Source        source;

  @Child protected AbstractDispatchNode nextInCache;

  @Child protected PreevaluatedExpression expr;

  public CachedExprNode(final DispatchGuard guard, final PreevaluatedExpression expr,
      final Source source, final AbstractDispatchNode nextInCache) {
    this.guard = guard;
    this.expr = expr;
    this.source = source;
    this.nextInCache = nextInCache;
  }

  @Override
  public Object executeDispatch(
      final VirtualFrame frame, final Object[] arguments) {
    Object rcvr = arguments[0];
    try {
      if (guard.entryMatches(rcvr)) {
        return expr.doPreEvaluated(frame, arguments);
      } else {
        return nextInCache.executeDispatch(frame, arguments);
      }
    } catch (InvalidAssumptionException e) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      return replace(SOMNode.unwrapIfNeeded(nextInCache)).executeDispatch(frame, arguments);
    }
  }

  @Override
  public int lengthOfDispatchChain() {
    return 1 + nextInCache.lengthOfDispatchChain();
  }

  @Override
  public Source getSource() {
    return source;
  }

  @SuppressWarnings("unchecked")
  @Override
  public AbstractDispatchNode initialize(final long sourceCoord) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getSourceCoordinate() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasSource() {
    return true;
  }
}
