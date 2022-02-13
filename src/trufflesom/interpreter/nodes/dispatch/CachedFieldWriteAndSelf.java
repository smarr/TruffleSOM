package trufflesom.interpreter.nodes.dispatch;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.nodes.SOMNode;
import trufflesom.interpreter.objectstorage.ObjectLayout;
import trufflesom.interpreter.objectstorage.StorageLocation;
import trufflesom.vmobjects.SObject;


public class CachedFieldWriteAndSelf extends AbstractDispatchWithSource {
  private final Class<?>        expectedClass;
  private final ObjectLayout    expectedLayout;
  private final StorageLocation storage;

  public CachedFieldWriteAndSelf(final Class<?> expectedClass,
      final ObjectLayout expectedLayout,
      final Source source, final StorageLocation storage, final AbstractDispatchNode next) {
    super(source, next);
    this.expectedClass = expectedClass;
    this.expectedLayout = expectedLayout;
    this.storage = storage;
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
}
