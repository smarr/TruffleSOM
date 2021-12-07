package trufflesom.interpreter.nodes.dispatch;

import static trufflesom.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import bd.primitives.nodes.PreevaluatedExpression;
import trufflesom.interpreter.Types;
import trufflesom.interpreter.nodes.GenericMessageSendNode;
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
      SClass rcvrClass = Types.getClassOf(rcvr);
      SInvokable method = rcvrClass.lookupInvokable(selector);

      UninitializedDispatchNode newChainEnd = new UninitializedDispatchNode(selector);
      DispatchGuard guard = DispatchGuard.create(rcvr);

      AbstractDispatchNode node;
      if (method == null) {
        node = new CachedDnuNode(rcvrClass, guard, selector, newChainEnd);
      } else {
        if (method.isTrivial()) {
          PreevaluatedExpression expr = method.copyTrivialNode();
          assert expr != null;
          node = new CachedExprNode(guard, expr, newChainEnd);
        } else {
          CallTarget callTarget = method.getCallTarget();
          node = new CachedDispatchNode(guard, callTarget, newChainEnd);
        }
      }

      return replace(node);
    }

    // the chain is longer than the maximum defined by INLINE_CACHE_SIZE and
    // thus, this callsite is considered to be megaprophic, and we generalize
    // it.
    GenericDispatchNode genericReplacement = new GenericDispatchNode(selector);
    GenericMessageSendNode sendNode = (GenericMessageSendNode) first.getParent();
    sendNode.replaceDispatchListHead(genericReplacement);
    return genericReplacement;
  }

  @Override
  public Object executeDispatch(final VirtualFrame frame, final Object[] arguments) {
    transferToInterpreterAndInvalidate("Initialize a dispatch node.");
    return specialize(arguments).executeDispatch(frame, arguments);
  }

  @Override
  public int lengthOfDispatchChain() {
    return 0;
  }
}
