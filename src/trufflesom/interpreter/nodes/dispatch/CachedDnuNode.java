package trufflesom.interpreter.nodes.dispatch;

import static trufflesom.vm.SymbolTable.symbolFor;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;

import trufflesom.interpreter.SArguments;
import trufflesom.interpreter.SomLanguage;
import trufflesom.interpreter.Types;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode.AbstractCachedDispatchNode;
import trufflesom.primitives.basics.SystemPrims.PrintStackTracePrim;
import trufflesom.vm.Universe;
import trufflesom.vm.VmSettings;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SSymbol;


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
        symbolFor("doesNotUnderstand:arguments:")).getCallTarget();
  }

  protected Object performDnu(final Object[] arguments, final Object rcvr) {
    if (VmSettings.PrintStackTraceOnDNU) {
      CompilerDirectives.transferToInterpreter();
      PrintStackTracePrim.printStackTrace(0, getSourceSection());
      Universe.errorPrintln("Lookup of " + selector + " failed in "
          + Types.getClassOf(rcvr, SomLanguage.getCurrentContext()).getName().getString());
    }

    return cachedMethod.call(rcvr, selector,
        SArguments.getArgumentsWithoutReceiver(arguments));
  }

  @Override
  public Object executeUnary(final VirtualFrame frame, final Object rcvr) {
    try {
      if (guard.entryMatches(rcvr)) {
        if (VmSettings.PrintStackTraceOnDNU) {
          CompilerDirectives.transferToInterpreter();
          PrintStackTracePrim.printStackTrace(0, getSourceSection());
          Universe.errorPrintln("Lookup of " + selector + " failed in "
              + Types.getClassOf(rcvr, SomLanguage.getCurrentContext()).getName().getString());
        }
        return cachedMethod.call3(rcvr, selector, SArray.create(0));
      } else {
        return nextInCache.executeUnary(frame, rcvr);
      }
    } catch (InvalidAssumptionException e) {
      CompilerDirectives.transferToInterpreter();
      return replace(nextInCache).executeUnary(frame, rcvr);
    }
  }

  @Override
  public Object executeBinary(final VirtualFrame frame, final Object rcvr, final Object arg) {
    try {
      if (guard.entryMatches(rcvr)) {
        if (VmSettings.PrintStackTraceOnDNU) {
          CompilerDirectives.transferToInterpreter();
          PrintStackTracePrim.printStackTrace(0, getSourceSection());
          Universe.errorPrintln("Lookup of " + selector + " failed in "
              + Types.getClassOf(rcvr, SomLanguage.getCurrentContext()).getName().getString());
        }
        return cachedMethod.call3(rcvr, selector, SArray.create(new Object[] {arg}));
      } else {
        return nextInCache.executeBinary(frame, rcvr, arg);
      }
    } catch (InvalidAssumptionException e) {
      CompilerDirectives.transferToInterpreter();
      return replace(nextInCache).executeBinary(frame, rcvr, arg);
    }
  }

  @Override
  public Object executeTernary(final VirtualFrame frame, final Object rcvr, final Object arg1,
      final Object arg2) {
    try {
      if (guard.entryMatches(rcvr)) {
        if (VmSettings.PrintStackTraceOnDNU) {
          CompilerDirectives.transferToInterpreter();
          PrintStackTracePrim.printStackTrace(0, getSourceSection());
          Universe.errorPrintln("Lookup of " + selector + " failed in "
              + Types.getClassOf(rcvr, SomLanguage.getCurrentContext()).getName().getString());
        }
        return cachedMethod.call3(rcvr, selector, SArray.create(new Object[] {arg1, arg2}));
      } else {
        return nextInCache.executeTernary(frame, rcvr, arg1, arg2);
      }
    } catch (InvalidAssumptionException e) {
      CompilerDirectives.transferToInterpreter();
      return replace(nextInCache).executeTernary(frame, rcvr, arg1, arg2);
    }
  }

  @Override
  public Object executeQuat(final VirtualFrame frame, final Object rcvr, final Object arg1,
      final Object arg2, final Object arg3) {
    try {
      if (guard.entryMatches(rcvr)) {
        if (VmSettings.PrintStackTraceOnDNU) {
          CompilerDirectives.transferToInterpreter();
          PrintStackTracePrim.printStackTrace(0, getSourceSection());
          Universe.errorPrintln("Lookup of " + selector + " failed in "
              + Types.getClassOf(rcvr, SomLanguage.getCurrentContext()).getName().getString());
        }
        return cachedMethod.call3(rcvr, selector,
            SArray.create(new Object[] {arg1, arg2, arg3}));
      } else {
        return nextInCache.executeQuat(frame, rcvr, arg1, arg2, arg3);
      }
    } catch (InvalidAssumptionException e) {
      CompilerDirectives.transferToInterpreter();
      return replace(nextInCache).executeQuat(frame, rcvr, arg1, arg2, arg3);
    }
  }
}
