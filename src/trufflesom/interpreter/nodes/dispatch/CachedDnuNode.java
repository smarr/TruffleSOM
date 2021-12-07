package trufflesom.interpreter.nodes.dispatch;

import static trufflesom.vm.SymbolTable.symbolFor;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;

import trufflesom.interpreter.SArguments;
import trufflesom.interpreter.Types;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode.AbstractCachedDispatchNode;
import trufflesom.primitives.basics.SystemPrims.PrintStackTracePrim;
import trufflesom.vm.Universe;
import trufflesom.vm.VmSettings;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SSymbol;


public final class CachedDnuNode extends AbstractCachedDispatchNode {
  private final SSymbol       selector;
  private final DispatchGuard guard;

  public CachedDnuNode(final SClass rcvrClass, final DispatchGuard guard,
      final SSymbol selector, final AbstractDispatchNode nextInCache) {
    super(getDnuCallTarget(rcvrClass), nextInCache);
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

  public static CallTarget getDnuCallTarget(final SClass rcvrClass) {
    return rcvrClass.lookupInvokable(
        symbolFor("doesNotUnderstand:arguments:")).getCallTarget();
  }

  protected Object performDnu(final Object[] arguments, final Object rcvr) {
    if (VmSettings.PrintStackTraceOnDNU) {
      CompilerDirectives.transferToInterpreter();
      PrintStackTracePrim.printStackTrace(0, getSourceSection());
      Universe.errorPrintln("Lookup of " + selector + " failed in "
          + Types.getClassOf(rcvr).getName().getString());
    }

    Object[] argsArr = new Object[] {
        rcvr, selector, SArguments.getArgumentsWithoutReceiver(arguments)};
    return cachedMethod.call(argsArr);
  }
}
