package som.interpreter.nodes.dispatch;
import static som.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;
import som.interpreter.nodes.MateMethodActivationNode;
import som.vm.constants.ExecutionLevel;
import som.vmobjects.SInvokable;
import som.vmobjects.SMateEnvironment;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;


public final class UninitializedMethodDispatchNode
    extends AbstractMethodDispatchNode {

  private AbstractMethodDispatchNode specialize(final SInvokable method) {
    transferToInterpreterAndInvalidate("Initialize a dispatch node.");

    // Determine position in dispatch node chain, i.e., size of inline cache
    Node i = this;
    int chainDepth = 0;
    while (i.getParent() instanceof AbstractMethodDispatchNode) {
      i = i.getParent();
      chainDepth++;
    }
    MateMethodActivationNode sendNode = (MateMethodActivationNode) i.getParent();
    
    if (chainDepth < INLINE_CACHE_SIZE) {
      AbstractMethodDispatchNode next = sendNode.getDispatchListHead();

      CachedMethodDispatchNode node;
      node = new CachedMethodDispatchNode(method, next);

      // the simple checks are prepended
      sendNode.adoptNewDispatchListHead(node);
      return node;
    } else {
      GenericMethodDispatchNode generic = new GenericMethodDispatchNode();
      sendNode.replaceDispatchListHead(generic);
      return generic;
    }
  }

  @Override
  public Object executeDispatch(final VirtualFrame frame, final SMateEnvironment environment, final ExecutionLevel exLevel, SInvokable method, final Object[] arguments) {
    return specialize(method).
        executeDispatch(frame, environment, exLevel, method, arguments);
  }

  @Override
  public int lengthOfDispatchChain() {
    return 0;
  }
}