package som.interpreter.nodes.dispatch;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;

import som.interpreter.nodes.dispatch.AbstractDispatchNode.AbstractCachedDispatchNode;


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
}
