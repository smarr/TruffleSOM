package trufflesom.interpreter.nodes.dispatch;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import trufflesom.interpreter.nodes.SOMNode;
import trufflesom.interpreter.objectstorage.ObjectLayout;
import trufflesom.interpreter.objectstorage.StorageLocation;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SObject;

public class CachedNewObject extends AbstractDispatchNode {
  private final ObjectLayout rcvrLayout;
  @CompilationFinal private Assumption isLatest;
  @CompilationFinal private ObjectLayout newInstanceLayout;
  private final Source source;

  @Node.Child
  protected AbstractDispatchNode nextInCache;

  public CachedNewObject(ObjectLayout rcvrLayout, final Assumption isLatest, final ObjectLayout newInstanceLayout,
      final Source source,  final AbstractDispatchNode next) {
    this.rcvrLayout = rcvrLayout;
    this.isLatest = isLatest;
    this.newInstanceLayout = newInstanceLayout;
    this.source = source;

    this.nextInCache = next;
  }

  @Override
  public int lengthOfDispatchChain() {
    return 1 + nextInCache.lengthOfDispatchChain();
  }

  @Override
  public Object executeDispatch(final VirtualFrame frame, final Object[] arguments) {
    Object rcvr = arguments[0];

    if (rcvr.getClass() == SClass.class) {
      try {
        rcvrLayout.checkIsLatest();
      } catch (InvalidAssumptionException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        return replace(SOMNode.unwrapIfNeeded(nextInCache)).executeDispatch(frame, arguments);
      }

      SClass clazz = ((SClass) rcvr);
      if (clazz.getObjectLayout() == rcvrLayout) {
        if (!isLatest.isValid()) {
          CompilerDirectives.transferToInterpreterAndInvalidate();
          ObjectLayout l = newInstanceLayout = clazz.getLayoutForInstances();
          isLatest = l.getAssumption();
        }

        return new SObject(clazz, newInstanceLayout);
      }
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
