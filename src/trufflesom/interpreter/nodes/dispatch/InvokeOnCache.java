package trufflesom.interpreter.nodes.dispatch;

import static trufflesom.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;

import trufflesom.vmobjects.SInvokable;


public abstract class InvokeOnCache extends Node implements DispatchChain {
  public static final int INLINE_CACHE_SIZE = 6;

  public static InvokeOnCache create() {
    return new UninitializedDispatchNode(0);
  }

  protected final int depth;

  public InvokeOnCache(final int depth) {
    this.depth = depth;
  }

  public abstract Object executeDispatch(VirtualFrame frame,
      SInvokable invokable, Object[] arguments);

  private static final class UninitializedDispatchNode extends InvokeOnCache {

    UninitializedDispatchNode(final int depth) {
      super(depth);
    }

    private InvokeOnCache specialize(final SInvokable invokable) {
      transferToInterpreterAndInvalidate("Initialize a dispatch node.");

      if (depth < INLINE_CACHE_SIZE) {
        CachedDispatchNode specialized = new CachedDispatchNode(invokable,
            new UninitializedDispatchNode(depth + 1),
            depth);
        return replace(specialized);
      }

      InvokeOnCache headNode = determineChainHead();
      GenericDispatchNode generic = new GenericDispatchNode();
      return headNode.replace(generic);
    }

    @Override
    public Object executeDispatch(final VirtualFrame frame,
        final SInvokable invokable, final Object[] arguments) {
      return specialize(invokable).executeDispatch(frame, invokable, arguments);
    }

    private InvokeOnCache determineChainHead() {
      Node i = this;
      while (i.getParent() instanceof InvokeOnCache) {
        i = i.getParent();
      }
      return (InvokeOnCache) i;
    }

    @Override
    public int lengthOfDispatchChain() {
      return 0;
    }
  }

  private static final class CachedDispatchNode extends InvokeOnCache {
    private final SInvokable      invokable;
    @Child private DirectCallNode callNode;
    @Child private InvokeOnCache  nextInCache;

    CachedDispatchNode(final SInvokable invokable,
        final InvokeOnCache nextInCache, final int depth) {
      super(depth);
      this.invokable = invokable;
      this.nextInCache = nextInCache;
      callNode = Truffle.getRuntime().createDirectCallNode(invokable.getCallTarget());
    }

    @Override
    public Object executeDispatch(final VirtualFrame frame,
        final SInvokable invokable, final Object[] arguments) {
      if (this.invokable == invokable) {
        return callNode.call(arguments);
      } else {
        return nextInCache.executeDispatch(frame, invokable, arguments);
      }
    }

    @Override
    public int lengthOfDispatchChain() {
      return 1 + nextInCache.lengthOfDispatchChain();
    }
  }

  private static final class GenericDispatchNode extends InvokeOnCache {

    @Child private IndirectCallNode callNode;

    GenericDispatchNode() {
      super(0);
      callNode = Truffle.getRuntime().createIndirectCallNode();
    }

    @Override
    public Object executeDispatch(final VirtualFrame frame,
        final SInvokable invokable, final Object[] arguments) {
      return callNode.call(invokable.getCallTarget(), arguments);
    }

    @Override
    public int lengthOfDispatchChain() {
      return 1000;
    }
  }
}
