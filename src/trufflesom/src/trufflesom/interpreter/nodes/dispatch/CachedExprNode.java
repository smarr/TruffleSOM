package trufflesom.interpreter.nodes.dispatch;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.source.Source;

import trufflesom.bdt.primitives.nodes.PreevaluatedExpression;
import trufflesom.interpreter.nodes.SOMNode;


public final class CachedExprNode extends AbstractDispatchWithSource {

  private final DispatchGuard             guard;
  @Child protected PreevaluatedExpression expr;

  public CachedExprNode(final DispatchGuard guard, final PreevaluatedExpression expr,
      final Source source, final AbstractDispatchNode nextInCache) {
    super(source, nextInCache);
    this.guard = guard;
    this.expr = expr;
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
}
