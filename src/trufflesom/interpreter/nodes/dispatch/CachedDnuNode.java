package som.interpreter.nodes.dispatch;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;

import som.interpreter.SArguments;
import som.interpreter.nodes.dispatch.AbstractDispatchNode.AbstractCachedDispatchNode;
import som.vm.Universe;
import som.vmobjects.SClass;
import som.vmobjects.SSymbol;


public final class CachedDnuNode extends AbstractCachedDispatchNode {
  private final SSymbol       selector;
  private final DispatchGuard guard;

  public CachedDnuNode(final SClass rcvrClass, final DispatchGuard guard,
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

  public static CallTarget getDnuCallTarget(final SClass rcvrClass, final Universe universe) {
    return rcvrClass.lookupInvokable(
        universe.symbolFor("doesNotUnderstand:arguments:")).getCallTarget();
  }

  protected Object performDnu(final Object[] arguments, final Object rcvr) {
    Object[] argsArr = new Object[] {
        rcvr, selector, SArguments.getArgumentsWithoutReceiver(arguments)};
    return cachedMethod.call(argsArr);
  }
}
