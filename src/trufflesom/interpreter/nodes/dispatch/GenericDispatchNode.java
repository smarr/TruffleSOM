package trufflesom.interpreter.nodes.dispatch;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;

import trufflesom.interpreter.SArguments;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode.GuardedDispatchNode;
import trufflesom.primitives.reflection.ObjectPrims.ClassPrim;
import trufflesom.primitives.reflection.ObjectPrimsFactory.ClassPrimFactory;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SSymbol;


public final class GenericDispatchNode extends GuardedDispatchNode {
  @Child private IndirectCallNode call;
  @Child private ClassPrim        classNode;

  protected final SSymbol selector;

  public GenericDispatchNode(final SSymbol selector, final Universe universe) {
    super(null);
    this.selector = selector;
    call = insert(Truffle.getRuntime().createIndirectCallNode());
    classNode = insert(ClassPrimFactory.create(null));
    classNode.initialize(universe);
  }

  @TruffleBoundary
  private Object sendDnu(final SClass rcvrClass, final Object[] arguments) {
    // Won't use DNU caching here, because it is already a megamorphic node
    SArray argumentsArray = SArguments.getArgumentsWithoutReceiver(arguments);
    Object[] args = new Object[] {arguments[0], selector, argumentsArray};
    CallTarget target = CachedDnuNode.getDnuCallTarget(rcvrClass);

    return call.call(target, args);
  }

  @Override
  public Object doPreEvaluated(final VirtualFrame frame, final Object[] arguments) {
    Object rcvr = arguments[0];
    SClass rcvrClass = classNode.executeEvaluated(rcvr);
    SInvokable method = rcvrClass.lookupInvokable(selector);
    if (method != null) {
      return call.call(method.getCallTarget(), arguments);
    }
    return sendDnu(rcvrClass, arguments);
  }

  @Override
  public boolean entryMatches(final Object rcvr) throws InvalidAssumptionException {
    return true;
  }
}
