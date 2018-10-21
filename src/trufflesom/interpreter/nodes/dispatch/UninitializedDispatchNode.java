package trufflesom.interpreter.nodes.dispatch;

import static som.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

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
    transferToInterpreterAndInvalidate("Initialize a dispatch node.");

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
      SClass rcvrClass = Types.getClassOf(rcvr, universe);
      SInvokable method = rcvrClass.lookupInvokable(selector);
      CallTarget callTarget;
      if (method != null) {
        callTarget = method.getCallTarget();
      } else {
        callTarget = null;
      }

      UninitializedDispatchNode newChainEnd =
          new UninitializedDispatchNode(selector, universe);
      DispatchGuard guard = DispatchGuard.create(rcvr);
      AbstractCachedDispatchNode node;
      if (method != null) {
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
    return specialize(arguments).executeDispatch(frame, arguments);
  }

  @Override
  public int lengthOfDispatchChain() {
    return 0;
  }
}
