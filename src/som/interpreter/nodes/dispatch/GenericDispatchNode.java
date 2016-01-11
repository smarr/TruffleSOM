package som.interpreter.nodes.dispatch;

import som.interpreter.SArguments;
import som.interpreter.Types;
import som.vm.constants.ExecutionLevel;
import som.vmobjects.SArray;
import som.vmobjects.SClass;
import som.vmobjects.SInvokable;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;

public final class GenericDispatchNode extends AbstractDispatchNode {
  @Child private IndirectCallNode call;
  protected final SSymbol selector;

  public GenericDispatchNode(final SSymbol selector) {
    this.selector = selector;
    call = Truffle.getRuntime().createIndirectCallNode();
  }

  @Override
  public Object executeDispatch(final VirtualFrame frame, 
      final SMateEnvironment environment, final ExecutionLevel exLevel, final Object[] arguments) {
    Object rcvr = arguments[0];
    DynamicObject rcvrClass = Types.getClassOf(rcvr);
    SInvokable method = SClass.lookupInvokable(rcvrClass, selector);

    CallTarget target;
    Object[] args;

    if (method != null) {
      target = method.getCallTarget();
      args = SArguments.createSArguments(environment, exLevel, arguments);
    } else {
      // Won't use DNU caching here, because it is already a megamorphic node
      SArray argumentsArray = SArguments.getArgumentsWithoutReceiver(arguments);
      args = new Object[] {environment, exLevel, arguments[SArguments.RCVR_ARGUMENTS_OFFSET], selector, argumentsArray};
      target = CachedDnuNode.getDnuCallTarget(rcvrClass);
    }
    return call.call(frame, target, args);
  }

  @Override
  public int lengthOfDispatchChain() {
    return 1000;
  }
}