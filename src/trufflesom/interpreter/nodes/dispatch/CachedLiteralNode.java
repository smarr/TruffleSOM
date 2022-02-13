package trufflesom.interpreter.nodes.dispatch;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.nodes.SOMNode;


public class CachedLiteralNode extends AbstractDispatchWithSource {

  private final DispatchGuard guard;
  private final Object        value;

  public CachedLiteralNode(final DispatchGuard guard, final Source source, final Object value,
      final AbstractDispatchNode next) {
    super(source, next);
    this.guard = guard;
    this.value = value;
  }

  @Override
  public Object executeDispatch(final VirtualFrame frame, final Object[] arguments) {
    try {
      Object rcvr = arguments[0];
      if (guard.entryMatches(rcvr)) {
        return value;
      }
    } catch (InvalidAssumptionException e) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      return replace(SOMNode.unwrapIfNeeded(nextInCache)).executeDispatch(frame, arguments);
    }
    return nextInCache.executeDispatch(frame, arguments);
  }
}
