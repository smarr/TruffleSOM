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
  public Object executeUnary(final VirtualFrame frame, final Object rcvr) {
    try {
      if (guard.entryMatches(rcvr)) {
        return expr.doPreUnary(frame, rcvr);
      } else {
        return nextInCache.executeUnary(frame, rcvr);
      }
    } catch (InvalidAssumptionException e) {
      CompilerDirectives.transferToInterpreter();
      return replace(nextInCache).executeUnary(frame, rcvr);
    }
  }

  @Override
  public Object executeBinary(final VirtualFrame frame, final Object rcvr, final Object arg) {
    try {
      if (guard.entryMatches(rcvr)) {
        return expr.doPreBinary(frame, rcvr, arg);
      } else {
        return nextInCache.executeBinary(frame, rcvr, arg);
      }
    } catch (InvalidAssumptionException e) {
      CompilerDirectives.transferToInterpreter();
      return replace(nextInCache).executeBinary(frame, rcvr, arg);
    }
  }

  @Override
  public Object executeTernary(final VirtualFrame frame, final Object rcvr, final Object arg1,
      final Object arg2) {
    try {
      if (guard.entryMatches(rcvr)) {
        return expr.doPreTernary(frame, rcvr, arg1, arg2);
      } else {
        return nextInCache.executeTernary(frame, rcvr, arg1, arg2);
      }
    } catch (InvalidAssumptionException e) {
      CompilerDirectives.transferToInterpreter();
      return replace(nextInCache).executeTernary(frame, rcvr, arg1, arg2);
    }
  }

  @Override
  public Object executeQuat(final VirtualFrame frame, final Object rcvr, final Object arg1,
      final Object arg2, final Object arg3) {
    try {
      if (guard.entryMatches(rcvr)) {
        return expr.doPreQuat(frame, rcvr, arg1, arg2, arg3);
      } else {
        return nextInCache.executeQuat(frame, rcvr, arg1, arg2, arg3);
      }
    } catch (InvalidAssumptionException e) {
      CompilerDirectives.transferToInterpreter();
      return replace(nextInCache).executeQuat(frame, rcvr, arg1, arg2, arg3);
    }
  }
}
