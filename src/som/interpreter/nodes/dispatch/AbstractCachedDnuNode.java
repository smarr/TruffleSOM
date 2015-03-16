package som.interpreter.nodes.dispatch;

import som.vm.Universe;
import som.vmobjects.SClass;

import com.oracle.truffle.api.CallTarget;


public final class AbstractCachedDnuNode {
  public static CallTarget getDnuCallTarget(final SClass rcvrClass) {
    return rcvrClass.lookupInvokable(
          Universe.current().symbolFor("doesNotUnderstand:arguments:")).
        getCallTarget();
  }
}
