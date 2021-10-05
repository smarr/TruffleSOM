package trufflesom.interpreter.nodes.dispatch;

import static trufflesom.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import bd.primitives.nodes.PreevaluatedExpression;
import trufflesom.interpreter.Types;
import trufflesom.interpreter.nodes.MessageSendNode.GenericMessageSendNode;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


public final class UninitializedDispatchNode extends AbstractDispatchNode {
  private final SSymbol  selector;
  private final Universe universe;

  public UninitializedDispatchNode(final SSymbol selector, final Universe universe) {
    this.selector = selector;
    this.universe = universe;
  }

  private AbstractDispatchNode specialize(final Object[] arguments) {
    return specializeObj(arguments[0]);
  }

  private AbstractDispatchNode specializeObj(final Object rcvr) {
    // Determine position in dispatch node chain, i.e., size of inline cache
    Node i = this;
    int chainDepth = 0;
    while (i.getParent() instanceof AbstractDispatchNode) {
      i = i.getParent();
      chainDepth++;
    }
    AbstractDispatchNode first = (AbstractDispatchNode) i;

    assert rcvr != null;

    if (rcvr instanceof SObject) {
      SObject r = (SObject) rcvr;
      if (r.updateLayoutToMatchClass() && first != this) { // if first is this, short cut and
                                                           // directly continue...
        return first;
      }
    }

    if (chainDepth < INLINE_CACHE_SIZE) {
      SClass rcvrClass = Types.getClassOf(rcvr, universe);
      SInvokable method = rcvrClass.lookupInvokable(selector);
      CallTarget callTarget = null;
      PreevaluatedExpression expr = null;
      if (method != null) {
        if (method.isTrivial()) {
          expr = method.copyTrivialNode();
          assert expr != null;
        } else {
          callTarget = method.getCallTarget();
        }
      }

      UninitializedDispatchNode newChainEnd =
          new UninitializedDispatchNode(selector, universe);
      DispatchGuard guard = DispatchGuard.create(rcvr);
      AbstractDispatchNode node;
      if (expr != null) {
        node = new CachedExprNode(guard, expr, newChainEnd);
      } else if (method != null) {
        node = new CachedDispatchNode(guard, callTarget, newChainEnd);
      } else {
        node = new CachedDnuNode(rcvrClass, guard, selector, newChainEnd, universe);
      }
      return replace(node);
    }

    // the chain is longer than the maximum defined by INLINE_CACHE_SIZE and
    // thus, this callsite is considered to be megaprophic, and we generalize
    // it.
    GenericDispatchNode genericReplacement = new GenericDispatchNode(selector, universe);
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

  @Override
  public Object executeBinary(final VirtualFrame frame, final Object rcvr, final Object arg) {
    transferToInterpreterAndInvalidate("Initialize a dispatch node.");
    return specializeObj(rcvr).executeBinary(frame, rcvr, arg);
  }
}
