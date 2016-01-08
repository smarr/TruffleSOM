package trufflesom.primitives.reflection;

import static trufflesom.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;

import trufflesom.interpreter.nodes.dispatch.DispatchChain;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.ReadFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.WriteFieldNode;
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

  public abstract Object executeDispatch(DynamicObject obj, int index);

  public abstract Object executeDispatch(DynamicObject obj, int index, Object value);

  private static final class UninitializedDispatchNode extends IndexDispatch {

    UninitializedDispatchNode(final int depth, final Universe universe) {
      super(depth, universe);
    }

    private IndexDispatch specialize(final DynamicObject clazz, final int index,
        final boolean read) {
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
    public Object executeDispatch(final DynamicObject obj, final int index) {
      return specialize(SObject.getSOMClass(obj), index, true).executeDispatch(obj,
          index);
    }

    @Override
    public Object executeDispatch(final DynamicObject obj, final int index,
        final Object value) {
      return specialize(SObject.getSOMClass(obj), index, false).executeDispatch(obj,
          index, value);
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
    private final DynamicObject  clazz;
    @Child private ReadFieldNode access;
    // TODO: have a second cached class for the writing...
    @Child private IndexDispatch next;

    CachedReadDispatchNode(final DynamicObject clazz, final int index,
        final IndexDispatch next, final int depth) {
      super(depth, next.universe);
      assert SClass.isSClass(clazz);
      this.index = index;
      this.clazz = clazz;
      this.next = next;
      access = FieldAccessorNode.createRead(index);
    }

    @Override
    public Object executeDispatch(final DynamicObject obj, final int index) {
      if (this.index == index && this.clazz == SObject.getSOMClass(obj)) {
        return access.executeRead(obj);
      } else {
        return next.executeDispatch(obj, index);
      }
    }

    @Override
    public Object executeDispatch(final DynamicObject obj, final int index,
        final Object value) {
      CompilerAsserts.neverPartOfCompilation();
      throw new RuntimeException("This should be never reached.");
    }

    @Override
    public int lengthOfDispatchChain() {
      return 1 + next.lengthOfDispatchChain();
    }
  }

  private static final class CachedWriteDispatchNode extends IndexDispatch {
    private final int             index;
    private final DynamicObject   clazz;
    @Child private WriteFieldNode access;
    @Child private IndexDispatch  next;

    CachedWriteDispatchNode(final DynamicObject clazz, final int index,
        final IndexDispatch next, final int depth) {
      super(depth, next.universe);
      assert SClass.isSClass(clazz);
      this.index = index;
      this.clazz = clazz;
      this.next = next;
      access = FieldAccessorNode.createWrite(index);
    }

    @Override
    public Object executeDispatch(final DynamicObject obj, final int index) {
      CompilerAsserts.neverPartOfCompilation();
      throw new RuntimeException("This should be never reached.");
    }

    @Override
    public Object executeDispatch(final DynamicObject obj, final int index,
        final Object value) {
      if (this.index == index && this.clazz == SObject.getSOMClass(obj)) {
        return access.executeWrite(obj, value);
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
    public Object executeDispatch(final DynamicObject obj, final int index) {
      return obj.get(index);
    }

    @Override
    public Object executeDispatch(final DynamicObject obj, final int index,
        final Object value) {
      obj.set(index, value);
      return value;
    }

    @Override
    public int lengthOfDispatchChain() {
      return 1000;
    }
  }
}
