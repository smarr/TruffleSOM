package trufflesom.interpreter.nodes.dispatch;

import static trufflesom.vm.SymbolTable.symbolFor;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;

import trufflesom.interpreter.SArguments;
import trufflesom.interpreter.SomLanguage;
import trufflesom.interpreter.Types;
import trufflesom.primitives.basics.SystemPrims.PrintStackTracePrim;
import trufflesom.vm.Universe;
import trufflesom.vm.VmSettings;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SSymbol;


public final class CachedDnuNode extends AbstractDispatchNode {
  private final SSymbol selector;

  @Child protected DirectCallNode cachedMethod;

  public CachedDnuNode(final SClass rcvrClass, final DispatchGuard guard,
      final SSymbol selector) {
    super(guard);
    this.selector = selector;

    cachedMethod = insert(Truffle.getRuntime().createDirectCallNode(
        getDnuCallTarget(rcvrClass)));
  }

  @Override
  public Object doPreEvaluated(final VirtualFrame frame, final Object[] arguments) {
    Object rcvr = arguments[0];
    if (VmSettings.PrintStackTraceOnDNU) {
      CompilerDirectives.transferToInterpreter();
      PrintStackTracePrim.printStackTrace(0, getSourceSection());
      Universe.errorPrintln("Lookup of " + selector + " failed in "
          + Types.getClassOf(rcvr, SomLanguage.getCurrentContext()).getName()
                 .getString());
    }

    Object[] argsArr = new Object[] {
        rcvr, selector, SArguments.getArgumentsWithoutReceiver(arguments)};
    return cachedMethod.call(argsArr);
  }

  public static CallTarget getDnuCallTarget(final SClass rcvrClass) {
    return rcvrClass.lookupInvokable(
        symbolFor("doesNotUnderstand:arguments:")).getCallTarget();
  }
}
