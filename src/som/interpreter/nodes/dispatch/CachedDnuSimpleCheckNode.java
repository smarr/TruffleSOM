package som.interpreter.nodes.dispatch;

import som.vm.constants.ExecutionLevel;
import som.vmobjects.SClass;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.frame.VirtualFrame;


public final class CachedDnuSimpleCheckNode extends AbstractCachedDnuNode {
  private final Class<?> expectedClass;

  public CachedDnuSimpleCheckNode(final Class<?> rcvrClass,
      final SClass rcvrSClass,
      final SSymbol selector, final AbstractDispatchNode nextInCache) {
    super(rcvrSClass, selector, nextInCache);
    expectedClass = rcvrClass;
  }

  @Override
  public Object executeDispatch(final VirtualFrame frame, final SMateEnvironment environment, final ExecutionLevel exLevel,final Object[] arguments) {
    Object rcvr = arguments[0];
    if (rcvr.getClass() == expectedClass) {
      return performDnu(frame, environment, exLevel, arguments, rcvr);
    } else {
      return nextInCache.executeDispatch(frame, environment, exLevel, arguments);
    }
  }
}
