package trufflesom.interpreter.objectstorage;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import trufflesom.interpreter.TypesGen;
import trufflesom.interpreter.nodes.SOMNode;
import trufflesom.interpreter.objectstorage.StorageLocation.AbstractObjectStorageLocation;
import trufflesom.interpreter.objectstorage.StorageLocation.DoubleStorageLocation;
import trufflesom.interpreter.objectstorage.StorageLocation.LongStorageLocation;
import trufflesom.vm.VmSettings;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SObject;


public abstract class FieldAccessorNode extends Node {
  protected final int fieldIndex;

  @InliningCutoff
  public static AbstractReadFieldNode createRead(final int fieldIndex) {
    return new UninitializedReadFieldNode(fieldIndex);
  }

  @InliningCutoff
  public static AbstractWriteFieldNode createWrite(final int fieldIndex) {
    return new UninitializedWriteFieldNode(fieldIndex);
  }

  @InliningCutoff
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

  public void notifyAsInserted() {
    if (VmSettings.UseInstrumentation) {
      notifyInserted(this);
    }
  }

  public abstract static class AbstractReadFieldNode extends FieldAccessorNode {
    public AbstractReadFieldNode(final int fieldIndex) {
      super(fieldIndex);
    }

    public abstract Object read(SObject obj);

    public long readLong(final SObject obj) throws UnexpectedResultException {
      return TypesGen.expectLong(read(obj));
    }

    public double readDouble(final SObject obj) throws UnexpectedResultException {
      return TypesGen.expectDouble(read(obj));
    }

    protected final Object specializeAndRead(final SObject obj, final String reason,
        final AbstractReadFieldNode next) {
      return specialize(obj, reason, next).read(obj);
    }

    public abstract boolean isLong(SObject obj);

    public LongStorageLocation getLongStorage(final SObject obj) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      throw new UnsupportedOperationException();
    }

    @InliningCutoff
    protected final AbstractReadFieldNode specialize(final SObject obj,
        final String reason, final AbstractReadFieldNode next) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      obj.updateLayoutToMatchClass();

      final ObjectLayout layout = obj.getObjectLayout();
      final StorageLocation location = layout.getStorageLocation(fieldIndex);

      AbstractReadFieldNode newNode = location.getReadNode(fieldIndex, layout, next);
      return replace(newNode, reason);
    }
  }

  public static final class UninitializedReadFieldNode extends AbstractReadFieldNode {

    public UninitializedReadFieldNode(final int fieldIndex) {
      super(fieldIndex);
    }

    @Override
    @InliningCutoff
    public Object read(final SObject obj) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      return specializeAndRead(obj, "uninitalized node",
          new UninitializedReadFieldNode(fieldIndex));
    }

    @Override
    public boolean isLong(final SObject obj) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      return specialize(obj, "uninitalized node",
          new UninitializedReadFieldNode(fieldIndex)).isLong(obj);
    }

    @Override
    public LongStorageLocation getLongStorage(final SObject obj) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      return specialize(obj, "uninitalized node",
          new UninitializedReadFieldNode(fieldIndex)).getLongStorage(obj);
    }
  }

  public abstract static class ReadSpecializedFieldNode extends AbstractReadFieldNode {
    protected final ObjectLayout           layout;
    @Child protected AbstractReadFieldNode nextInCache;

    public ReadSpecializedFieldNode(final int fieldIndex,
        final ObjectLayout layout, final AbstractReadFieldNode next) {
      super(fieldIndex);
      this.layout = layout;
      nextInCache = next;
    }

    protected final boolean hasExpectedLayout(final SObject obj)
        throws InvalidAssumptionException {
      layout.checkIsLatest();
      return layout == obj.getObjectLayout();
    }

    protected final AbstractReadFieldNode respecializedNodeOrNext(final SObject obj) {
      if (layout.layoutForSameClass(obj.getObjectLayout())) {
        return specialize(obj, "update outdated read node", nextInCache);
      } else {
        return nextInCache;
      }
    }
  }

  public static final class ReadUnwrittenFieldNode extends ReadSpecializedFieldNode {
    public ReadUnwrittenFieldNode(final int fieldIndex,
        final ObjectLayout layout, final AbstractReadFieldNode next) {
      super(fieldIndex, layout, next);
    }

    @Override
    public Object read(final SObject obj) {
      try {
        if (hasExpectedLayout(obj)) {
          return Nil.nilObject;
        } else {
          return respecializedNodeOrNext(obj).read(obj);
        }
      } catch (InvalidAssumptionException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        return dropAndReadNext(obj);
      }
    }

    @Override
    public boolean isLong(final SObject obj) {
      try {
        if (hasExpectedLayout(obj)) {
          return false;
        } else {
          return respecializedNodeOrNext(obj).isLong(obj);
        }
      } catch (InvalidAssumptionException e) {
        return replace(SOMNode.unwrapIfNeeded(nextInCache)).isLong(obj);
      }
    }

    @InliningCutoff
    private Object dropAndReadNext(final SObject obj) {
      return replace(SOMNode.unwrapIfNeeded(nextInCache)).read(obj);
    }
  }

  public static final class ReadLongFieldNode extends ReadSpecializedFieldNode {
    private final LongStorageLocation storage;

    public ReadLongFieldNode(final int fieldIndex, final ObjectLayout layout,
        final AbstractReadFieldNode next) {
      super(fieldIndex, layout, next);
      this.storage = (LongStorageLocation) layout.getStorageLocation(fieldIndex);
    }

    @Override
    public long readLong(final SObject obj) throws UnexpectedResultException {
      try {
        if (hasExpectedLayout(obj)) {
          return storage.readLong(obj);
        } else {
          return respecializedNodeOrNext(obj).readLong(obj);
        }
      } catch (InvalidAssumptionException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        return dropAndReadNext(obj);
      }
    }

    @InliningCutoff
    private long dropAndReadNext(final SObject obj) throws UnexpectedResultException {
      return replace(SOMNode.unwrapIfNeeded(nextInCache)).readLong(obj);
    }

    @Override
    public Object read(final SObject obj) {
      try {
        return readLong(obj);
      } catch (UnexpectedResultException e) {
        return e.getResult();
      }
    }

    @Override
    public boolean isLong(final SObject obj) {
      try {
        if (hasExpectedLayout(obj)) {
          return true;
        } else {
          return respecializedNodeOrNext(obj).isLong(obj);
        }
      } catch (InvalidAssumptionException e) {
        return replace(SOMNode.unwrapIfNeeded(nextInCache)).isLong(obj);
      }
    }

    @Override
    public LongStorageLocation getLongStorage(final SObject obj) {
      try {
        if (hasExpectedLayout(obj)) {
          return storage;
        } else {
          return respecializedNodeOrNext(obj).getLongStorage(obj);
        }
      } catch (InvalidAssumptionException e) {
        return replace(SOMNode.unwrapIfNeeded(nextInCache)).getLongStorage(obj);
      }
    }
  }

  public static final class ReadDoubleFieldNode extends ReadSpecializedFieldNode {
    private final DoubleStorageLocation storage;

    public ReadDoubleFieldNode(final int fieldIndex, final ObjectLayout layout,
        final AbstractReadFieldNode next) {
      super(fieldIndex, layout, next);
      this.storage = (DoubleStorageLocation) layout.getStorageLocation(fieldIndex);
    }

    @Override
    public double readDouble(final SObject obj) throws UnexpectedResultException {
      try {
        if (hasExpectedLayout(obj)) {
          return storage.readDouble(obj);
        } else {
          return respecializedNodeOrNext(obj).readDouble(obj);
        }
      } catch (InvalidAssumptionException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        return dropAndReadNext(obj);
      }
    }

    @InliningCutoff
    private double dropAndReadNext(final SObject obj) throws UnexpectedResultException {
      return replace(SOMNode.unwrapIfNeeded(nextInCache)).readDouble(obj);
    }

    @Override
    public Object read(final SObject obj) {
      try {
        return readDouble(obj);
      } catch (UnexpectedResultException e) {
        return e.getResult();
      }
    }

    @Override
    public boolean isLong(final SObject obj) {
      try {
        if (hasExpectedLayout(obj)) {
          return false;
        } else {
          return respecializedNodeOrNext(obj).isLong(obj);
        }
      } catch (InvalidAssumptionException e) {
        return replace(SOMNode.unwrapIfNeeded(nextInCache)).isLong(obj);
      }
    }
  }

  public static final class ReadObjectFieldNode extends ReadSpecializedFieldNode {
    private final AbstractObjectStorageLocation storage;

    public ReadObjectFieldNode(final int fieldIndex, final ObjectLayout layout,
        final AbstractReadFieldNode next) {
      super(fieldIndex, layout, next);
      this.storage = (AbstractObjectStorageLocation) layout.getStorageLocation(fieldIndex);
    }

    @Override
    public Object read(final SObject obj) {
      try {
        if (hasExpectedLayout(obj)) {
          return storage.read(obj);
        } else {
          return respecializedNodeOrNext(obj).read(obj);
        }
      } catch (InvalidAssumptionException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        return dropAndReadNext(obj);
      }
    }

    @Override
    public boolean isLong(final SObject obj) {
      try {
        if (hasExpectedLayout(obj)) {
          return false;
        } else {
          return respecializedNodeOrNext(obj).isLong(obj);
        }
      } catch (InvalidAssumptionException e) {
        return replace(SOMNode.unwrapIfNeeded(nextInCache)).isLong(obj);
      }
    }

    @InliningCutoff
    private Object dropAndReadNext(final SObject obj) {
      return replace(SOMNode.unwrapIfNeeded(nextInCache)).read(obj);
    }
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

    @InliningCutoff
    protected final void writeAndRespecialize(final SObject obj, final Object value,
        final String reason, final AbstractWriteFieldNode next) {
      CompilerDirectives.transferToInterpreterAndInvalidate();

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
    @InliningCutoff
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

    public long increment(final SObject obj, final long incValue) {
      try {
        if (hasExpectedLayout(obj)) {
          return storage.increment(obj, incValue);
        } else {
          ensureNext(obj);
          return nextInCache.increment(obj, incValue);
        }
      } catch (InvalidAssumptionException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        ensureNext(obj);
        return dropAndIncrementNext(obj, incValue);
      }
    }

    @InliningCutoff
    private long dropAndIncrementNext(final SObject obj, final long incValue) {
      return replace(SOMNode.unwrapIfNeeded(nextInCache)).increment(obj, incValue);
    }

    @InliningCutoff
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
        CompilerDirectives.transferToInterpreterAndInvalidate();
        dropAndWriteNext(obj, value);
      }
      return value;
    }

    @InliningCutoff
    private void dropAndWriteNext(final SObject obj, final long value) {
      replace(SOMNode.unwrapIfNeeded(nextInCache)).write(obj, value);
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
        CompilerDirectives.transferToInterpreterAndInvalidate();
        dropAndWriteNext(obj, value);
      }
      return value;
    }

    @InliningCutoff
    private void dropAndWriteNext(final SObject obj, final double value) {
      replace(SOMNode.unwrapIfNeeded(nextInCache)).write(obj, value);
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
        CompilerDirectives.transferToInterpreterAndInvalidate();
        dropAndWriteNext(obj, value);
      }
      return value;
    }

    @InliningCutoff
    private void dropAndWriteNext(final SObject obj, final Object value) {
      replace(SOMNode.unwrapIfNeeded(nextInCache)).write(obj, value);
    }
  }
}
