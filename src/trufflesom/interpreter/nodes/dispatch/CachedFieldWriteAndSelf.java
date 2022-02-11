package trufflesom.interpreter.nodes.dispatch;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import trufflesom.interpreter.nodes.SOMNode;
import trufflesom.interpreter.objectstorage.ObjectLayout;
import trufflesom.interpreter.objectstorage.StorageLocation;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SObject;

public class CachedFieldWriteAndSelf extends AbstractDispatchNode {
  private final Class<?>        expectedClass;
  private final ObjectLayout expectedLayout;
  private final Source source;
  private final StorageLocation storage;

  @Node.Child
  protected AbstractDispatchNode nextInCache;

  public CachedFieldWriteAndSelf(final Class<?> expectedClass, final ObjectLayout expectedLayout,
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
      Object value = arguments[1];

      if (rcvr.getClass() == expectedClass) {
        SObject receiver = (SObject) rcvr;
        if (receiver.getObjectLayout() == expectedLayout) {
          storage.write(receiver, value);
          return rcvr;
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
