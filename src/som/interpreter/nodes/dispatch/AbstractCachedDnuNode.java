package som.interpreter.nodes.dispatch;

import som.interpreter.SArguments;
import som.interpreter.nodes.dispatch.AbstractDispatchNode.AbstractCachedDispatchNode;
import som.vm.Universe;
import som.vm.constants.ExecutionLevel;
import som.vmobjects.SClass;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;


public abstract class AbstractCachedDnuNode extends AbstractCachedDispatchNode {
  private final SSymbol selector;

  public static CallTarget getDnuCallTarget(final SClass rcvrClass) {
    return rcvrClass.lookupInvokable(
          Universe.current().symbolFor("doesNotUnderstand:arguments:")).
        getCallTarget();
  }

  public AbstractCachedDnuNode(final SClass rcvrClass,
      final SSymbol selector, final AbstractDispatchNode nextInCache) {
    super(getDnuCallTarget(rcvrClass), nextInCache);
    this.selector = selector;
  }

  protected final Object performDnu(final VirtualFrame frame, final SMateEnvironment environment, final ExecutionLevel exLevel, final Object[] arguments,
      final Object rcvr) {
    Object[] argsArr = new Object[] {
        environment, exLevel, rcvr, selector, SArguments.getArgumentsWithoutReceiver(arguments) };
    return cachedMethod.call(frame, argsArr);
  }
}
