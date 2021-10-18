package trufflesom.interpreter.nodes.dispatch;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;

import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode.AbstractCachedDispatchNode;


public final class CachedDispatchNode extends AbstractCachedDispatchNode {

  private final DispatchGuard guard;

  public CachedDispatchNode(final DispatchGuard guard,
      final CallTarget callTarget, final AbstractDispatchNode nextInCache) {
    super(callTarget, nextInCache);
    this.guard = guard;
  }

  @Override
  public Object executeDispatch(
      final VirtualFrame frame, final Object[] arguments) {
    Object rcvr = arguments[0];
    try {
      if (guard.entryMatches(rcvr)) {
        return cachedMethod.call(arguments);
      } else {
        return nextInCache.executeDispatch(frame, arguments);
      }
    } catch (InvalidAssumptionException e) {
      CompilerDirectives.transferToInterpreter();
      return replace(nextInCache).executeDispatch(frame, arguments);
    }
  }

  @Override
  public Object executeUnary(final VirtualFrame frame, final Object rcvr) {
    try {
      if (guard.entryMatches(rcvr)) {
        return cachedMethod.call1(rcvr);
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
        return cachedMethod.call2(rcvr, arg);
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
        return cachedMethod.call3(rcvr, arg1, arg2);
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
        return cachedMethod.call4(rcvr, arg1, arg2, arg3);
      } else {
        return nextInCache.executeQuat(frame, rcvr, arg1, arg2, arg3);
      }
    } catch (InvalidAssumptionException e) {
      CompilerDirectives.transferToInterpreter();
      return replace(nextInCache).executeQuat(frame, rcvr, arg1, arg2, arg3);
    }
  }
}
