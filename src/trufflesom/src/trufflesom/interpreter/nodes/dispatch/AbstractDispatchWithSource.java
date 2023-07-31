package trufflesom.interpreter.nodes.dispatch;

import com.oracle.truffle.api.source.Source;

import trufflesom.bdt.inlining.nodes.WithSource;


public abstract class AbstractDispatchWithSource extends AbstractDispatchNode
    implements WithSource {
  private final Source source;

  @Child protected AbstractDispatchNode nextInCache;

  public AbstractDispatchWithSource(final Source source, final AbstractDispatchNode next) {
    this.nextInCache = next;
    this.source = source;
  }

  @Override
  public final int lengthOfDispatchChain() {
    return 1 + nextInCache.lengthOfDispatchChain();
  }

  @Override
  public final Source getSource() {
    return source;
  }

  @SuppressWarnings("unchecked")
  @Override
  public final AbstractDispatchNode initialize(final long sourceCoord) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final long getSourceCoordinate() {
    throw new UnsupportedOperationException();
  }

  @Override
  public final boolean hasSource() {
    return true;
  }
}
