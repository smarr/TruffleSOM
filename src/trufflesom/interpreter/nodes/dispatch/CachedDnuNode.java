package trufflesom.interpreter.nodes.dispatch;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.object.DynamicObject;

import trufflesom.interpreter.SArguments;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode.AbstractCachedDispatchNode;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SSymbol;


public final class CachedDnuNode extends AbstractCachedDispatchNode {
  private final SSymbol       selector;
  private final DispatchGuard guard;

  public CachedDnuNode(final DynamicObject rcvrClass, final DispatchGuard guard,
      final SSymbol selector, final AbstractDispatchNode nextInCache,
      final Universe universe) {
    super(getDnuCallTarget(rcvrClass, universe), nextInCache);
    this.selector = selector;
    this.guard = guard;
  }

  @Override
  public Object executeDispatch(final VirtualFrame frame, final Object[] arguments) {
    Object rcvr = arguments[0];
    try {
      if (guard.entryMatches(rcvr)) {
        return performDnu(arguments, rcvr);
      } else {
        return nextInCache.executeDispatch(frame, arguments);
      }
    } catch (InvalidAssumptionException e) {
      CompilerDirectives.transferToInterpreter();
      return replace(nextInCache).executeDispatch(frame, arguments);
    }
  }

  public static CallTarget getDnuCallTarget(final DynamicObject rcvrClass,
      final Universe universe) {
    return SClass.lookupInvokable(rcvrClass,
        universe.symbolFor("doesNotUnderstand:arguments:"), universe).getCallTarget();
  }

  protected Object performDnu(final Object[] arguments, final Object rcvr) {
    Object[] argsArr = new Object[] {
        rcvr, selector, SArguments.getArgumentsWithoutReceiver(arguments)};
    return cachedMethod.call(argsArr);
  }
}
