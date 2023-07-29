package trufflesom.interpreter.nodes.dispatch;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import trufflesom.bdt.primitives.nodes.PreevaluatedExpression;
import trufflesom.interpreter.Types;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


public final class UninitializedDispatchNode extends AbstractDispatchNode {
  private final SSymbol selector;

  public UninitializedDispatchNode(final SSymbol selector) {
    this.selector = selector;
  }

  private AbstractDispatchNode specialize(final Object[] arguments) {
    // Determine position in dispatch node chain, i.e., size of inline cache
    Node i = this;
    int chainDepth = 0;
    while (i.getParent() instanceof AbstractDispatchNode) {
      i = i.getParent();
      chainDepth++;
    }
    AbstractDispatchNode first = (AbstractDispatchNode) i;

    Object rcvr = arguments[0];
    assert rcvr != null;

    if (rcvr instanceof SObject) {
      SObject r = (SObject) rcvr;
      if (r.updateLayoutToMatchClass() && first != this) { // if first is this, short cut and
                                                           // directly continue...
        return first;
      }
    }

    if (chainDepth < INLINE_CACHE_SIZE) {
      UninitializedDispatchNode newChainEnd = new UninitializedDispatchNode(selector);
      AbstractDispatchNode node = createDispatch(rcvr, selector, newChainEnd);

      replace(node);
      newChainEnd.notifyAsInserted();
      return node;
    }

    // the chain is longer than the maximum defined by INLINE_CACHE_SIZE and
    // thus, this callsite is considered to be megaprophic, and we generalize it.
    GenericDispatchNode genericReplacement = new GenericDispatchNode(selector);
    first.replace(genericReplacement);
    return genericReplacement;
  }

  public static AbstractDispatchNode createDispatch(final Object rcvr, final SSymbol selector,
      final UninitializedDispatchNode newChainEnd) {
    SClass rcvrClass = Types.getClassOf(rcvr);
    SInvokable method = rcvrClass.lookupInvokable(selector);

    if (method == null) {
      DispatchGuard guard = DispatchGuard.create(rcvr);
      return new CachedDnuNode(rcvrClass, guard, selector, newChainEnd);
    }

    AbstractDispatchNode node = method.asDispatchNode(rcvr, newChainEnd);
    if (node != null) {
      return node;
    }

    PreevaluatedExpression expr = method.copyTrivialNode();

    DispatchGuard guard = DispatchGuard.create(rcvr);
    if (expr != null) {
      return new CachedExprNode(guard, expr, method.getSource(), newChainEnd);
    }

    CallTarget callTarget = method.getCallTarget();
    return new CachedDispatchNode(guard, callTarget, newChainEnd);
  }

  @Override
  public Object executeDispatch(final VirtualFrame frame, final Object[] arguments) {
    transferToInterpreterAndInvalidate();
    return specialize(arguments).executeDispatch(frame, arguments);
  }

  @Override
  public int lengthOfDispatchChain() {
    return 0;
  }
}
