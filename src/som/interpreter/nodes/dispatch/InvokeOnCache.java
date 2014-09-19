package som.interpreter.nodes.dispatch;

import som.vmobjects.SInvokable;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;


public abstract class InvokeOnCache extends Node implements DispatchChain {
  public static final int INLINE_CACHE_SIZE = 6;

  public static InvokeOnCache create() {
    return new GenericDispatchNode();
  }

  protected final int depth;

  public InvokeOnCache(final int depth) {
    this.depth = depth;
  }

  public abstract Object executeDispatch(VirtualFrame frame,
      SInvokable invokable, Object[] arguments);

  private static final class GenericDispatchNode extends InvokeOnCache {

    @Child private IndirectCallNode callNode;

    public GenericDispatchNode() {
      super(0);
      callNode = Truffle.getRuntime().createIndirectCallNode();
    }

    @Override
    public Object executeDispatch(final VirtualFrame frame,
        final SInvokable invokable, final Object[] arguments) {
      return callNode.call(frame, invokable.getCallTarget(), arguments);
    }

    @Override
    public int lengthOfDispatchChain() {
      return 1000;
    }
  }
}
