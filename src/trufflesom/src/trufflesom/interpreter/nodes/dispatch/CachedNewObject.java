package trufflesom.interpreter.nodes.dispatch;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.nodes.SOMNode;
import trufflesom.interpreter.objectstorage.ObjectLayout;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SObject;


public class CachedNewObject extends AbstractDispatchWithSource {
  private final ObjectLayout             rcvrLayout;
  @CompilationFinal private Assumption   isLatest;
  @CompilationFinal private ObjectLayout newInstanceLayout;

  public CachedNewObject(final ObjectLayout rcvrLayout, final Assumption isLatest,
      final ObjectLayout newInstanceLayout,
      final Source source, final AbstractDispatchNode next) {
    super(source, next);
    this.rcvrLayout = rcvrLayout;
    this.isLatest = isLatest;
    this.newInstanceLayout = newInstanceLayout;
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
}
