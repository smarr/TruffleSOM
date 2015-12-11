package som.interpreter.nodes.dispatch;
import static som.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;
import som.primitives.BlockPrims.ValuePrimitiveNode;
import som.vm.constants.ExecutionLevel;
import som.vmobjects.SBlock;
import som.vmobjects.SInvokable;
import som.vmobjects.SMateEnvironment;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;


public final class UnitializedMethodDispatchNode
    extends AbstractDispatchNode {

  private AbstractDispatchNode specialize(final SBlock rcvr) {
    transferToInterpreterAndInvalidate("Initialize a dispatch node.");

    // Determine position in dispatch node chain, i.e., size of inline cache
    Node i = this;
    int chainDepth = 0;
    while (i.getParent() instanceof AbstractDispatchNode) {
      i = i.getParent();
      chainDepth++;
    }
    ValuePrimitiveNode primitiveNode = (ValuePrimitiveNode) i.getParent();

    if (chainDepth < INLINE_CACHE_SIZE) {
      SInvokable method = rcvr.getMethod();

      assert method != null;

      UnitializedMethodDispatchNode uninitialized = new UnitializedMethodDispatchNode();
      CachedMethodDispatchNode node = new CachedMethodDispatchNode(method, uninitialized);
      return replace(node);
    } else {
      GenericBlockDispatchNode generic = new GenericBlockDispatchNode();
      primitiveNode.adoptNewDispatchListHead(generic);
      return generic;
    }
  }

  @Override
  public Object executeDispatch(final VirtualFrame frame, final SMateEnvironment environment, final ExecutionLevel exLevel, final Object[] arguments) {
    return specialize((SBlock) arguments[0]).
        executeDispatch(frame, environment, exLevel, arguments);
  }

  @Override
  public int lengthOfDispatchChain() {
    return 0;
  }
}