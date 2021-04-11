package trufflesom.interpreter.nodes.dispatch;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;

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
  @Child private ClassPrim        classNode;

  protected final SSymbol selector;
  private final Universe  universe;

  public GenericDispatchNode(final SSymbol selector, final Universe universe) {
    this.selector = selector;
    this.universe = universe;
    call = Truffle.getRuntime().createIndirectCallNode();
    classNode = ClassPrimFactory.create(null);
    classNode.initialize(universe);
  }

  @TruffleBoundary
  private Object dispatch(final Object[] arguments) {
    Object rcvr = arguments[0];
    SClass rcvrClass = classNode.executeEvaluated(rcvr);
    SInvokable method = rcvrClass.lookupInvokable(selector);

    CallTarget target;
    Object[] args;

    if (method != null) {
      target = method.getCallTarget();
      args = arguments;
    } else {
      // TODO: actually do use node
      CompilerDirectives.transferToInterpreter();
      // Won't use DNU caching here, because it is already a megamorphic node
      SArray argumentsArray = SArguments.getArgumentsWithoutReceiver(arguments);
      args = new Object[] {arguments[0], selector, argumentsArray};
      target = CachedDnuNode.getDnuCallTarget(rcvrClass, universe);
    }
    return call.call(target, args);
  }

  @Override
  public Object executeDispatch(
      final VirtualFrame frame, final Object[] arguments) {
    return dispatch(arguments);
  }

  @Override
  public int lengthOfDispatchChain() {
    return 1000;
  }
}
