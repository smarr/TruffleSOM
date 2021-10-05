package trufflesom.interpreter.nodes.dispatch;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;

import bd.primitives.nodes.PreevaluatedExpression;


public class CachedExprNode extends AbstractDispatchNode {

  private final DispatchGuard guard;

  @Child protected AbstractDispatchNode nextInCache;

  @Child protected PreevaluatedExpression expr;

  public CachedExprNode(final DispatchGuard guard, final PreevaluatedExpression expr,
      final AbstractDispatchNode nextInCache) {
    this.guard = guard;
    this.expr = expr;
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
      CompilerDirectives.transferToInterpreter();
      return replace(nextInCache).executeDispatch(frame, arguments);
    }
  }

  @Override
  public final int lengthOfDispatchChain() {
    return 1 + nextInCache.lengthOfDispatchChain();
  }

  @Override
  public Object executeBinary(final VirtualFrame frame, final Object rcvr, final Object arg) {
    try {
      if (guard.entryMatches(rcvr)) {
        return expr.doPreEvaluated(frame, new Object[] {rcvr, arg});
      } else {
        return nextInCache.executeBinary(frame, rcvr, arg);
      }
    } catch (InvalidAssumptionException e) {
      CompilerDirectives.transferToInterpreter();
      return replace(nextInCache).executeBinary(frame, rcvr, arg);
    }
  }
}
