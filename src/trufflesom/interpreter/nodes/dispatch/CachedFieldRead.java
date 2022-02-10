package trufflesom.interpreter.nodes.dispatch;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.nodes.SOMNode;
import trufflesom.interpreter.objectstorage.ObjectLayout;
import trufflesom.interpreter.objectstorage.StorageLocation;
import trufflesom.vmobjects.SObject;


public final class CachedFieldRead extends AbstractDispatchNode {

  private final Class<?>        expectedClass;
  private final ObjectLayout    expectedLayout;
  private final Source          source;
  private final StorageLocation storage;

  @Child protected AbstractDispatchNode nextInCache;

  public CachedFieldRead(final Class<?> expectedClass, final ObjectLayout expectedLayout,
      final Source source, final StorageLocation storage, final AbstractDispatchNode next) {
    this.expectedClass = expectedClass;
    this.expectedLayout = expectedLayout;
    this.source = source;
    this.storage = storage;

    this.nextInCache = next;
  }

  @Override
  public int lengthOfDispatchChain() {
    return 1 + nextInCache.lengthOfDispatchChain();
  }

  @Override
  public Object executeDispatch(final VirtualFrame frame, final Object[] arguments) {
    try {
      expectedLayout.checkIsLatest();
      Object rcvr = arguments[0];

      if (rcvr.getClass() == expectedClass) {
        SObject receiver = (SObject) rcvr;
        if (receiver.getObjectLayout() == expectedLayout) {
          return storage.read(receiver);
        }
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
