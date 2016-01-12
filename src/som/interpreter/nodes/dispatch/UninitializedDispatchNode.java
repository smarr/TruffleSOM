package som.interpreter.nodes.dispatch;

import static som.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;
import som.interpreter.Types;
import som.interpreter.nodes.MessageSendNode.GenericMessageSendNode;
import som.vm.constants.ExecutionLevel;
import som.vmobjects.SClass;
import som.vmobjects.SInvokable;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;


public final class UninitializedDispatchNode extends AbstractDispatchNode {
  protected final SSymbol selector;

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

    /*if (rcvr instanceof DynamicObject) {
      DynamicObject r = (DynamicObject) rcvr;
      if (r.updateShape() && first != this) { // if first is this, short cut and directly continue...
        return first;
      }
    }*/
    
    if (chainDepth < INLINE_CACHE_SIZE) {
      DynamicObject rcvrClass = Types.getClassOf(rcvr);
      SInvokable method = SClass.lookupInvokable(rcvrClass, selector);
      CallTarget callTarget;
      if (method != null) {
        callTarget = method.getCallTarget();
      } else {
        callTarget = null;
      }

      UninitializedDispatchNode newChainEnd = new UninitializedDispatchNode(selector);
      DispatchGuard guard = DispatchGuard.create(rcvr);
      AbstractCachedDispatchNode node;
      if (method != null) {
        node = new CachedDispatchNode(guard, callTarget, newChainEnd);
      } else {
        node = new CachedDnuNode(rcvrClass, guard, selector, newChainEnd);
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
  public Object executeDispatch(final VirtualFrame frame, 
      final DynamicObject environment, final ExecutionLevel exLevel, final Object[] arguments) {
    transferToInterpreterAndInvalidate("Initialize a dispatch node.");
    return specialize(arguments).
    executeDispatch(frame, environment, exLevel, arguments);
  }

  @Override
  public int lengthOfDispatchChain() {
    return 0;
  }
}