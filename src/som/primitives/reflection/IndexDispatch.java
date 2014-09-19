package som.primitives.reflection;

import som.interpreter.nodes.dispatch.DispatchChain;
import som.vmobjects.SObject;

import com.oracle.truffle.api.nodes.Node;


public abstract class IndexDispatch extends Node implements DispatchChain {
  public static final int INLINE_CACHE_SIZE = 6;

  public static IndexDispatch create() {
    return new GenericDispatchNode();
  }

  protected final int depth;

  public IndexDispatch(final int depth) {
    this.depth = depth;
  }

  public abstract Object executeDispatch(SObject obj, int index);
  public abstract Object executeDispatch(SObject obj, int index, Object value);

  private static final class GenericDispatchNode extends IndexDispatch {

    public GenericDispatchNode() {
      super(0);
    }

    @Override
    public Object executeDispatch(final SObject obj, final int index) {
      return obj.getField(index);
    }

    @Override
    public Object executeDispatch(final SObject obj, final int index, final Object value) {
      obj.setField(index, value);
      return value;
    }

    @Override
    public int lengthOfDispatchChain() {
      return 1000;
    }
  }
}
