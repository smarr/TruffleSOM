package trufflesom.interpreter.objectstorage;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.nodes.Node;

import trufflesom.interpreter.TruffleCompiler;
import trufflesom.interpreter.objectstorage.StorageLocation.AbstractObjectStorageLocation;
import trufflesom.interpreter.objectstorage.StorageLocation.DoubleStorageLocation;
import trufflesom.interpreter.objectstorage.StorageLocation.LongStorageLocation;
import trufflesom.vmobjects.SObject;


public abstract class FieldAccessorNode extends Node {
  protected final int fieldIndex;

  public static AbstractWriteFieldNode createWrite(final int fieldIndex) {
    return new UninitializedWriteFieldNode(fieldIndex);
  }

  public static IncrementLongFieldNode createIncrement(final int fieldIndex,
      final SObject obj) {
    final ObjectLayout layout = obj.getObjectLayout();
    return new IncrementLongFieldNode(fieldIndex, layout);
  }

  private FieldAccessorNode(final int fieldIndex) {
    this.fieldIndex = fieldIndex;
  }

  public final int getFieldIndex() {
    return fieldIndex;
  }

  public abstract static class AbstractWriteFieldNode extends FieldAccessorNode {
    public AbstractWriteFieldNode(final int fieldIndex) {
      super(fieldIndex);
    }

    public abstract Object write(SObject obj, Object value);

    public long write(final SObject obj, final long value) {
      write(obj, (Object) value);
      return value;
    }

    public double write(final SObject obj, final double value) {
      write(obj, (Object) value);
      return value;
    }

    protected final void writeAndRespecialize(final SObject obj, final Object value,
        final String reason, final AbstractWriteFieldNode next) {
      TruffleCompiler.transferToInterpreterAndInvalidate(reason);

      obj.setField(fieldIndex, value);

      final ObjectLayout layout = obj.getObjectLayout();
      final StorageLocation location = layout.getStorageLocation(fieldIndex);
      AbstractWriteFieldNode newNode = location.getWriteNode(fieldIndex, layout, next);
      replace(newNode, reason);
    }
  }

  private static final class UninitializedWriteFieldNode extends AbstractWriteFieldNode {
    UninitializedWriteFieldNode(final int fieldIndex) {
      super(fieldIndex);
    }

    @Override
    public Object write(final SObject obj, final Object value) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      writeAndRespecialize(obj, value, "initialize write field node",
          new UninitializedWriteFieldNode(fieldIndex));
      return value;
    }
  }

  private abstract static class WriteSpecializedFieldNode extends AbstractWriteFieldNode {

    protected final ObjectLayout            layout;
    @Child protected AbstractWriteFieldNode nextInCache;

    WriteSpecializedFieldNode(final int fieldIndex,
        final ObjectLayout layout, final AbstractWriteFieldNode next) {
      super(fieldIndex);
      this.layout = layout;
      nextInCache = next;
    }

    protected final boolean hasExpectedLayout(final SObject obj)
        throws InvalidAssumptionException {
      layout.checkIsLatest();
      return layout == obj.getObjectLayout();
    }
  }

  public static final class IncrementLongFieldNode extends FieldAccessorNode {
    protected final ObjectLayout      layout;
    private final LongStorageLocation storage;

    @Child protected IncrementLongFieldNode nextInCache;

    public IncrementLongFieldNode(final int fieldIndex, final ObjectLayout layout) {
      super(fieldIndex);
      this.layout = layout;
      this.storage = (LongStorageLocation) layout.getStorageLocation(fieldIndex);
    }

    protected boolean hasExpectedLayout(final SObject obj)
        throws InvalidAssumptionException {
      layout.checkIsLatest();
      return layout == obj.getObjectLayout();
    }

    public long increment(final SObject obj) {
      try {
        if (hasExpectedLayout(obj)) {
          return storage.increment(obj);
        } else {
          ensureNext(obj);
          return nextInCache.increment(obj);
        }
      } catch (InvalidAssumptionException e) {
        ensureNext(obj);
        return replace(nextInCache).increment(obj);
      }
    }

    private void ensureNext(final SObject obj) {
      if (nextInCache == null) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        nextInCache = new IncrementLongFieldNode(fieldIndex, obj.getObjectLayout());
      }
    }
  }

  public static final class WriteLongFieldNode extends WriteSpecializedFieldNode {
    private final LongStorageLocation storage;

    public WriteLongFieldNode(final int fieldIndex, final ObjectLayout layout,
        final AbstractWriteFieldNode next) {
      super(fieldIndex, layout, next);
      this.storage = (LongStorageLocation) layout.getStorageLocation(fieldIndex);
    }

    @Override
    public long write(final SObject obj, final long value) {
      try {
        if (hasExpectedLayout(obj)) {
          storage.writeLong(obj, value);
        } else {
          if (layout.layoutForSameClass(obj.getObjectLayout())) {
            writeAndRespecialize(obj, value, "update outdated write node", nextInCache);
          } else {
            nextInCache.write(obj, value);
          }
        }
      } catch (InvalidAssumptionException e) {
        replace(nextInCache).write(obj, value);
      }
      return value;
    }

    @Override
    public Object write(final SObject obj, final Object value) {
      if (value instanceof Long) {
        write(obj, (long) value);
      } else {
        if (layout.layoutForSameClass(obj.getObjectLayout())) {
          writeAndRespecialize(obj, value, "update outdated read node", nextInCache);
        } else {
          nextInCache.write(obj, (long) value);
        }
      }
      return value;
    }
  }

  public static final class WriteDoubleFieldNode extends WriteSpecializedFieldNode {
    private final DoubleStorageLocation storage;

    public WriteDoubleFieldNode(final int fieldIndex, final ObjectLayout layout,
        final AbstractWriteFieldNode next) {
      super(fieldIndex, layout, next);
      this.storage = (DoubleStorageLocation) layout.getStorageLocation(fieldIndex);
    }

    @Override
    public double write(final SObject obj, final double value) {
      try {
        if (hasExpectedLayout(obj)) {
          storage.writeDouble(obj, value);
        } else {
          if (layout.layoutForSameClass(obj.getObjectLayout())) {
            writeAndRespecialize(obj, value, "update outdated read node", nextInCache);
          } else {
            nextInCache.write(obj, value);
          }
        }
      } catch (InvalidAssumptionException e) {
        replace(nextInCache).write(obj, value);
      }
      return value;
    }

    @Override
    public Object write(final SObject obj, final Object value) {
      if (value instanceof Double) {
        write(obj, (double) value);
      } else {
        if (layout.layoutForSameClass(obj.getObjectLayout())) {
          writeAndRespecialize(obj, value, "update outdated read node", nextInCache);
        } else {
          nextInCache.write(obj, (double) value);
        }
      }
      return value;
    }
  }

  public static final class WriteObjectFieldNode extends WriteSpecializedFieldNode {
    private final AbstractObjectStorageLocation storage;

    public WriteObjectFieldNode(final int fieldIndex, final ObjectLayout layout,
        final AbstractWriteFieldNode next) {
      super(fieldIndex, layout, next);
      this.storage = (AbstractObjectStorageLocation) layout.getStorageLocation(fieldIndex);
    }

    @Override
    public Object write(final SObject obj, final Object value) {
      try {
        if (hasExpectedLayout(obj)) {
          storage.write(obj, value);
        } else {
          if (layout.layoutForSameClass(obj.getObjectLayout())) {
            writeAndRespecialize(obj, value, "update outdated read node", nextInCache);
          } else {
            nextInCache.write(obj, value);
          }
        }
      } catch (InvalidAssumptionException e) {
        replace(nextInCache).write(obj, value);
      }
      return value;
    }
  }
}
