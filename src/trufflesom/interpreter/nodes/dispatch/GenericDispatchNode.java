package trufflesom.interpreter.nodes.dispatch;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;

import trufflesom.interpreter.SArguments;
import trufflesom.primitives.reflection.ObjectPrims.ClassPrim;
import trufflesom.primitives.reflection.ObjectPrimsFactory.ClassPrimFactory;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SSymbol;


public final class GenericDispatchNode extends AbstractDispatchNode {
  @Child private IndirectCallNode call;
  @Child private ClassPrim        getClass;
  protected final SSymbol         selector;
  private final Universe          universe;

  public GenericDispatchNode(final SSymbol selector, final Universe universe) {
    this.selector = selector;
    this.universe = universe;
    call = Truffle.getRuntime().createIndirectCallNode();
    getClass = ClassPrimFactory.create(null);
    getClass.initialize(universe);
  }

  @Override
  public Object executeDispatch(
      final VirtualFrame frame, final Object[] arguments) {
    Universe.callerNeedsToBeOptimized("We should not reach this, ideally, in benchmark code");

    Object rcvr = arguments[0];
    DynamicObject rcvrClass = (DynamicObject) getClass.executeEvaluated(null, rcvr);
    SInvokable method = SClass.lookupInvokable(rcvrClass, selector, universe);

    CallTarget target;
    Object[] args;

    if (method != null) {
      target = method.getCallTarget();
      args = arguments;
    } else {
      // Won't use DNU caching here, because it is already a megamorphic node
      SArray argumentsArray = SArguments.getArgumentsWithoutReceiver(arguments);
      args = new Object[] {arguments[0], selector, argumentsArray};
      target = CachedDnuNode.getDnuCallTarget(rcvrClass, universe);
    }
    return call.call(target, args);
  }

  @Override
  public int lengthOfDispatchChain() {
    return 1000;
  }
}
