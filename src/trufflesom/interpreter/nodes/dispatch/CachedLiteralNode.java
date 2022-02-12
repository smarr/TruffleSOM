package trufflesom.interpreter.nodes.dispatch;

import bd.inlining.nodes.WithSource;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import trufflesom.interpreter.nodes.SOMNode;

public class CachedLiteralNode extends AbstractDispatchNode implements WithSource {

  private final DispatchGuard guard;
  private final Source source;
  private final Object value;

  @Node.Child
  protected AbstractDispatchNode nextInCache;

  public CachedLiteralNode(final DispatchGuard guard, final Source source, Object value, final AbstractDispatchNode next) {
    this.guard = guard;
    this.source = source;
    this.value = value;

    this.nextInCache = next;
  }

  @Override
  public int lengthOfDispatchChain() {
    return 1 + nextInCache.lengthOfDispatchChain();
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
