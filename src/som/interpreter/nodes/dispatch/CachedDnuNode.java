package som.interpreter.nodes.dispatch;

import som.interpreter.SArguments;
import som.interpreter.nodes.dispatch.AbstractDispatchNode.AbstractCachedDispatchNode;
import som.vm.Universe;
import som.vm.constants.ExecutionLevel;
import som.vmobjects.SClass;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.object.DynamicObject;

public final class CachedDnuNode extends AbstractCachedDispatchNode {
  private final SSymbol selector;
  private final DispatchGuard guard;

  public CachedDnuNode(final DynamicObject rcvrClass, final DispatchGuard guard,
      final SSymbol selector, final AbstractDispatchNode nextInCache) {
    super(getDnuCallTarget(rcvrClass), nextInCache);
    this.selector = selector;
    this.guard = guard;
  }

  @Override
  public Object executeDispatch(final VirtualFrame frame, 
      final SMateEnvironment environment, final ExecutionLevel exLevel, final Object[] arguments) {
    Object rcvr = arguments[0];
    try {
      if (guard.entryMatches(rcvr)) {
        return performDnu(frame, environment, exLevel, arguments, rcvr);
      } else {
        return nextInCache.executeDispatch(frame, environment, exLevel, arguments);
      }
    } catch (InvalidAssumptionException e) {
      CompilerDirectives.transferToInterpreter();
      return replace(nextInCache).
          executeDispatch(frame, environment, exLevel, arguments);
    }
  }

  public static CallTarget getDnuCallTarget(final DynamicObject rcvrClass) {
    return SClass.lookupInvokable(rcvrClass,
          Universe.current().symbolFor("doesNotUnderstand:arguments:")).
        getCallTarget();
  }

  protected final Object performDnu(final VirtualFrame frame, 
      final SMateEnvironment environment, final ExecutionLevel exLevel, final Object[] arguments,
      final Object rcvr) {
    Object[] argsArr = new Object[] {
        environment, exLevel, rcvr, selector, SArguments.getArgumentsWithoutReceiver(arguments) };
    return cachedMethod.call(frame, argsArr);
  }
}