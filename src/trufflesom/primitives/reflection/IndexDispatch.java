package trufflesom.primitives.reflection;

import static trufflesom.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;

import trufflesom.interpreter.nodes.FieldNode.FieldReadNode;
import trufflesom.interpreter.nodes.FieldNodeFactory.FieldReadNodeGen;
import trufflesom.interpreter.nodes.dispatch.DispatchChain;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SObject;


public abstract class IndexDispatch extends Node implements DispatchChain {
  public static final int INLINE_CACHE_SIZE = 6;

  public static IndexDispatch create(final Universe universe) {
    return new UninitializedDispatchNode(0, universe);
  }

  protected final int      depth;
  protected final Universe universe;

  public IndexDispatch(final int depth, final Universe universe) {
    this.depth = depth;
    this.universe = universe;
  }

  public abstract Object executeDispatch(SObject obj, int index);

  public abstract Object executeDispatch(SObject obj, int index, Object value);

  private static final class UninitializedDispatchNode extends IndexDispatch {

    UninitializedDispatchNode(final int depth, final Universe universe) {
      super(depth, universe);
    }

    @TruffleBoundary
    private IndexDispatch specialize(final SClass clazz, final int index, final boolean read) {
      transferToInterpreterAndInvalidate("Initialize a dispatch node.");

      if (depth < INLINE_CACHE_SIZE) {
        IndexDispatch specialized;
        if (read) {
          specialized = new CachedReadDispatchNode(clazz, index,
              new UninitializedDispatchNode(depth + 1, universe), depth);
        } else {
          specialized = new CachedWriteDispatchNode(clazz, index,
              new UninitializedDispatchNode(depth + 1, universe), depth);
        }
        return replace(specialized);
      }

      IndexDispatch headNode = determineChainHead();
      return headNode.replace(new GenericDispatchNode());
    }

    @Override
    public Object executeDispatch(final SObject obj, final int index) {
      return specialize(obj.getSOMClass(universe), index, true).executeDispatch(obj, index);
    }

    @Override
    public Object executeDispatch(final SObject obj, final int index, final Object value) {
      return specialize(obj.getSOMClass(universe), index, false).executeDispatch(obj, index,
          value);
    }

    private IndexDispatch determineChainHead() {
      Node i = this;
      while (i.getParent() instanceof IndexDispatch) {
        i = i.getParent();
      }
      return (IndexDispatch) i;
    }

    @Override
    public int lengthOfDispatchChain() {
      return 0;
    }
  }

  private static final class CachedReadDispatchNode extends IndexDispatch {
    private final int            index;
    private final SClass         clazz;
    @Child private FieldReadNode access;
    // TODO: have a second cached class for the writing...
    @Child private IndexDispatch next;

    CachedReadDispatchNode(final SClass clazz, final int index,
        final IndexDispatch next, final int depth) {
      super(depth, next.universe);
      this.index = index;
      this.clazz = clazz;
      this.next = next;
      access = FieldReadNodeGen.create(index, null);
    }

    @Override
    public Object executeDispatch(final SObject obj, final int index) {
      if (this.index == index && this.clazz == obj.getSOMClass(universe)) {
        return access.executeEvaluated(obj);
      } else {
        return next.executeDispatch(obj, index);
      }
    }

    @TruffleBoundary
    @Override
    public Object executeDispatch(final SObject obj, final int index, final Object value) {
      CompilerAsserts.neverPartOfCompilation();
      throw new RuntimeException("This should be never reached.");
    }

    @Override
    public int lengthOfDispatchChain() {
      return 1 + next.lengthOfDispatchChain();
    }
  }

  private static final class CachedWriteDispatchNode extends IndexDispatch {
    private final int                     index;
    private final SClass                  clazz;
    @Child private AbstractWriteFieldNode access;
    @Child private IndexDispatch          next;

    CachedWriteDispatchNode(final SClass clazz, final int index, final IndexDispatch next,
        final int depth) {
      super(depth, next.universe);
      this.index = index;
      this.clazz = clazz;
      this.next = next;
      access = FieldAccessorNode.createWrite(index);
    }

    @Override
    @TruffleBoundary
    public Object executeDispatch(final SObject obj, final int index) {
      CompilerAsserts.neverPartOfCompilation();
      throw new RuntimeException("This should be never reached.");
    }

    @Override
    public Object executeDispatch(final SObject obj, final int index, final Object value) {
      if (this.index == index && this.clazz == obj.getSOMClass(universe)) {
        return access.write(obj, value);
      } else {
        return next.executeDispatch(obj, index, value);
      }
    }

    @Override
    public int lengthOfDispatchChain() {
      return 1 + next.lengthOfDispatchChain();
    }
  }

  private static final class GenericDispatchNode extends IndexDispatch {

    GenericDispatchNode() {
      super(0, null);
    }

    @Override
    @TruffleBoundary
    public Object executeDispatch(final SObject obj, final int index) {
      return obj.getField(index);
    }

    @Override
    @TruffleBoundary
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
