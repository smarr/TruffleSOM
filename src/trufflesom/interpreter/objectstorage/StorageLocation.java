package trufflesom.interpreter.objectstorage;

import java.lang.reflect.Field;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import trufflesom.interpreter.TruffleCompiler;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.ReadDoubleFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.ReadLongFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.ReadObjectFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.ReadUnwrittenFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.WriteDoubleFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.WriteLongFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.WriteObjectFieldNode;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SObject;
import sun.misc.Unsafe;


public abstract class StorageLocation {
  private static Unsafe loadUnsafe() {
    try {
      return Unsafe.getUnsafe();
    } catch (SecurityException e) {}
    try {
      Field theUnsafeInstance = Unsafe.class.getDeclaredField("theUnsafe");
      theUnsafeInstance.setAccessible(true);
      return (Unsafe) theUnsafeInstance.get(Unsafe.class);
    } catch (Exception e) {
      throw new RuntimeException(
          "exception while trying to get Unsafe.theUnsafe via reflection:", e);
    }
  }

  private static final Unsafe unsafe = loadUnsafe();

  public static long getFieldOffset(final Field field) {
    return unsafe.objectFieldOffset(field);
  }

  public interface LongStorageLocation {
    long readLong(SObject obj) throws UnexpectedResultException;

    void writeLong(SObject obj, long value);
  }

  public interface DoubleStorageLocation {
    double readDouble(SObject obj) throws UnexpectedResultException;

    void writeDouble(SObject obj, double value);
  }

  public static StorageLocation createForLong(final ObjectLayout layout,
      final long fieldIndex, final int primFieldIndex) {
    CompilerAsserts.neverPartOfCompilation("StorageLocation");
    if (primFieldIndex < SObject.NUM_PRIMITIVE_FIELDS) {
      return new LongDirectStoreLocation(layout, fieldIndex, primFieldIndex);
    } else {
      return new LongArrayStoreLocation(layout, fieldIndex, primFieldIndex);
    }
  }

  public static StorageLocation createForDouble(final ObjectLayout layout,
      final long fieldIndex, final int primFieldIndex) {
    CompilerAsserts.neverPartOfCompilation("StorageLocation");
    if (primFieldIndex < SObject.NUM_PRIMITIVE_FIELDS) {
      return new DoubleDirectStoreLocation(layout, fieldIndex, primFieldIndex);
    } else {
      return new DoubleArrayStoreLocation(layout, fieldIndex, primFieldIndex);
    }
  }

  public static StorageLocation createForObject(final ObjectLayout layout,
      final int objFieldIndex) {
    CompilerAsserts.neverPartOfCompilation("StorageLocation");
    if (objFieldIndex < SObject.NUM_PRIMITIVE_FIELDS) {
      return new ObjectDirectStorageLocation(layout, objFieldIndex);
    } else {
      return new ObjectArrayStorageLocation(layout, objFieldIndex);
    }
  }

  private final ObjectLayout layout;    // for debugging only
  protected final long       fieldIndex;

  protected StorageLocation(final ObjectLayout layout, final long fieldIndex) {
    this.layout = layout;
    this.fieldIndex = fieldIndex;
  }

  public abstract boolean isSet(SObject obj);

  public abstract Object read(SObject obj);

  public abstract void write(SObject obj, Object value);

  public abstract AbstractReadFieldNode getReadNode(int fieldIndex, ObjectLayout layout,
      AbstractReadFieldNode next);

  public abstract AbstractWriteFieldNode getWriteNode(int fieldIndex, ObjectLayout layout,
      AbstractWriteFieldNode next);

  public static final class UnwrittenStorageLocation extends StorageLocation {

    public UnwrittenStorageLocation(final ObjectLayout layout, final long index) {
      super(layout, index);
    }

    @Override
    public boolean isSet(final SObject obj) {
      return false;
    }

    @Override
    public Object read(final SObject obj) {
      CompilerAsserts.neverPartOfCompilation("StorageLocation");
      return Nil.nilObject;
    }

    @Override
    public void write(final SObject obj, final Object value) {
      CompilerAsserts.neverPartOfCompilation("StorageLocation");
      obj.setUninitializedField(fieldIndex, value);
    }

    @Override
    public AbstractReadFieldNode getReadNode(final int fieldIndex,
        final ObjectLayout layout, final AbstractReadFieldNode next) {
      CompilerAsserts.neverPartOfCompilation("StorageLocation");
      return new ReadUnwrittenFieldNode(fieldIndex, layout, next);
    }

    @Override
    public AbstractWriteFieldNode getWriteNode(final int fieldIndex,
        final ObjectLayout layout, final AbstractWriteFieldNode next) {
      CompilerAsserts.neverPartOfCompilation("StorageLocation");
      throw new RuntimeException("we should not get here, should we?");
      // return new UninitializedWriteFieldNode(fieldIndex);
    }
  }

  public abstract static class AbstractObjectStorageLocation extends StorageLocation {

    public AbstractObjectStorageLocation(final ObjectLayout layout, final int fieldIndex) {
      super(layout, fieldIndex);
    }

    @Override
    public abstract void write(SObject obj, Object value);

    @Override
    public final AbstractReadFieldNode getReadNode(final int fieldIndex,
        final ObjectLayout layout, final AbstractReadFieldNode next) {
      CompilerAsserts.neverPartOfCompilation("StorageLocation");
      return new ReadObjectFieldNode(fieldIndex, layout, next);
    }

    @Override
    public final AbstractWriteFieldNode getWriteNode(final int fieldIndex,
        final ObjectLayout layout, final AbstractWriteFieldNode next) {
      CompilerAsserts.neverPartOfCompilation("StorageLocation");
      return new WriteObjectFieldNode(fieldIndex, layout, next);
    }
  }

  public static final class ObjectDirectStorageLocation extends AbstractObjectStorageLocation {
    private final long fieldOffset;

    public ObjectDirectStorageLocation(final ObjectLayout layout, final int fieldIndex) {
      super(layout, fieldIndex);
      fieldOffset = SObject.getObjectFieldOffset(fieldIndex);
    }

    @Override
    public boolean isSet(final SObject obj) {
      assert read(obj) != null;
      return true;
    }

    @Override
    public Object read(final SObject obj) {
      return unsafe.getObject(obj, fieldOffset);
    }

    @Override
    public void write(final SObject obj, final Object value) {
      assert value != null;
      unsafe.putObject(obj, fieldOffset, value);
    }
  }

  public static final class ObjectArrayStorageLocation extends AbstractObjectStorageLocation {
    private final int extensionIndex;

    public ObjectArrayStorageLocation(final ObjectLayout layout, final int fieldIndex) {
      super(layout, fieldIndex);
      extensionIndex = fieldIndex - SObject.NUM_OBJECT_FIELDS;
    }

    @Override
    public boolean isSet(final SObject obj) {
      assert read(obj) != null;
      return true;
    }

    @Override
    public Object read(final SObject obj) {
      Object[] arr = obj.getExtensionObjFields();
      return arr[extensionIndex];
    }

    @Override
    public void write(final SObject obj, final Object value) {
      assert value != null;
      Object[] arr = obj.getExtensionObjFields();
      arr[extensionIndex] = value;
    }
  }

  public abstract static class PrimitiveStorageLocation extends StorageLocation {
    protected final int mask;

    protected PrimitiveStorageLocation(final ObjectLayout layout,
        final long fieldIndex, final int primField) {
      super(layout, fieldIndex);
      mask = SObject.getPrimitiveFieldMask(primField);
    }

    @Override
    public final boolean isSet(final SObject obj) {
      return obj.isPrimitiveSet(mask);
    }

    protected final void markAsSet(final SObject obj) {
      obj.markPrimAsSet(mask);
    }
  }

  public abstract static class PrimitiveDirectStoreLocation extends PrimitiveStorageLocation {
    protected final long offset;

    public PrimitiveDirectStoreLocation(final ObjectLayout layout,
        final long fieldIndex, final int primField) {
      super(layout, fieldIndex, primField);
      offset = SObject.getPrimitiveFieldOffset(primField);
    }
  }

  public static final class DoubleDirectStoreLocation extends PrimitiveDirectStoreLocation
      implements DoubleStorageLocation {
    public DoubleDirectStoreLocation(final ObjectLayout layout,
        final long fieldIndex, final int primField) {
      super(layout, fieldIndex, primField);
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
    public double readDouble(final SObject obj) throws UnexpectedResultException {
      if (isSet(obj)) {
        return unsafe.getDouble(obj, offset);
      } else {
        TruffleCompiler.transferToInterpreterAndInvalidate("unstabelized read node");
        throw new UnexpectedResultException(Nil.nilObject);
      }
    }

    @Override
    public void write(final SObject obj, final Object value) {
      assert value != null;
      if (value instanceof Double) {
        writeDouble(obj, (double) value);
      } else {
        assert value != Nil.nilObject;
        TruffleCompiler.transferToInterpreter("unstabelized read node");
        obj.setFieldAndGeneralize(fieldIndex, value);
      }
    }

    @Override
    public void writeDouble(final SObject obj, final double value) {
      unsafe.putDouble(obj, offset, value);
      markAsSet(obj);
    }

    @Override
    public AbstractReadFieldNode getReadNode(final int fieldIndex,
        final ObjectLayout layout, final AbstractReadFieldNode next) {
      CompilerAsserts.neverPartOfCompilation("StorageLocation");
      return new ReadDoubleFieldNode(fieldIndex, layout, next);
    }

    @Override
    public AbstractWriteFieldNode getWriteNode(final int fieldIndex,
        final ObjectLayout layout, final AbstractWriteFieldNode next) {
      CompilerAsserts.neverPartOfCompilation("StorageLocation");
      return new WriteDoubleFieldNode(fieldIndex, layout, next);
    }
  }

  public static final class LongDirectStoreLocation extends PrimitiveDirectStoreLocation
      implements LongStorageLocation {

    public LongDirectStoreLocation(final ObjectLayout layout,
        final long fieldIndex, final int primField) {
      super(layout, fieldIndex, primField);
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
    public long readLong(final SObject obj) throws UnexpectedResultException {
      if (isSet(obj)) {
        return unsafe.getLong(obj, offset);
      } else {
        TruffleCompiler.transferToInterpreter("unstabelized read node");
        throw new UnexpectedResultException(Nil.nilObject);
      }
    }

    @Override
    public void write(final SObject obj, final Object value) {
      assert value != null;
      if (value instanceof Long) {
        writeLong(obj, (long) value);
      } else {
        TruffleCompiler.transferToInterpreter("unstabelized write node");
        obj.setFieldAndGeneralize(fieldIndex, value);
      }
    }

    @Override
    public void writeLong(final SObject obj, final long value) {
      unsafe.putLong(obj, offset, value);
      markAsSet(obj);
    }

    @Override
    public AbstractReadFieldNode getReadNode(final int fieldIndex,
        final ObjectLayout layout, final AbstractReadFieldNode next) {
      CompilerAsserts.neverPartOfCompilation("StorageLocation");
      return new ReadLongFieldNode(fieldIndex, layout, next);
    }

    @Override
    public AbstractWriteFieldNode getWriteNode(final int fieldIndex,
        final ObjectLayout layout, final AbstractWriteFieldNode next) {
      CompilerAsserts.neverPartOfCompilation("StorageLocation");
      return new WriteLongFieldNode(fieldIndex, layout, next);
    }
  }

  public abstract static class PrimitiveArrayStoreLocation extends PrimitiveStorageLocation {
    protected final int extensionIndex;

    public PrimitiveArrayStoreLocation(final ObjectLayout layout,
        final long fieldIndex, final int primField) {
      super(layout, fieldIndex, primField);
      extensionIndex = primField - SObject.NUM_PRIMITIVE_FIELDS;
      assert extensionIndex >= 0;
    }
  }

  public static final class LongArrayStoreLocation extends PrimitiveArrayStoreLocation
      implements LongStorageLocation {
    public LongArrayStoreLocation(final ObjectLayout layout,
        final long fieldIndex, final int primField) {
      super(layout, fieldIndex, primField);
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
    public long readLong(final SObject obj) throws UnexpectedResultException {
      if (isSet(obj)) {
        // perhaps we should use the unsafe operations as for doubles
        return obj.getExtendedPrimFields()[extensionIndex];
      } else {
        TruffleCompiler.transferToInterpreterAndInvalidate("unstabelized read node");
        throw new UnexpectedResultException(Nil.nilObject);
      }
    }

    @Override
    public void write(final SObject obj, final Object value) {
      assert value != null;
      if (value instanceof Long) {
        writeLong(obj, (long) value);
      } else {
        assert value != Nil.nilObject;
        TruffleCompiler.transferToInterpreterAndInvalidate("unstabelized write node");
        obj.setFieldAndGeneralize(fieldIndex, value);
      }
    }

    @Override
    public void writeLong(final SObject obj, final long value) {
      obj.getExtendedPrimFields()[extensionIndex] = value;
      markAsSet(obj);
    }

    @Override
    public AbstractReadFieldNode getReadNode(final int fieldIndex,
        final ObjectLayout layout, final AbstractReadFieldNode next) {
      CompilerAsserts.neverPartOfCompilation("StorageLocation");
      return new ReadLongFieldNode(fieldIndex, layout, next);
    }

    @Override
    public AbstractWriteFieldNode getWriteNode(final int fieldIndex,
        final ObjectLayout layout, final AbstractWriteFieldNode next) {
      CompilerAsserts.neverPartOfCompilation("StorageLocation");
      return new WriteLongFieldNode(fieldIndex, layout, next);
    }
  }

  public static final class DoubleArrayStoreLocation extends PrimitiveArrayStoreLocation
      implements DoubleStorageLocation {
    public DoubleArrayStoreLocation(final ObjectLayout layout,
        final long fieldIndex, final int primField) {
      super(layout, fieldIndex, primField);
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
    public double readDouble(final SObject obj) throws UnexpectedResultException {
      if (isSet(obj)) {
        long[] arr = obj.getExtendedPrimFields();
        return unsafe.getDouble(arr,
            (long) Unsafe.ARRAY_DOUBLE_BASE_OFFSET
                + Unsafe.ARRAY_DOUBLE_INDEX_SCALE * extensionIndex);
      } else {
        TruffleCompiler.transferToInterpreterAndInvalidate("unstabelized read node");
        throw new UnexpectedResultException(Nil.nilObject);
      }
    }

    @Override
    public void write(final SObject obj, final Object value) {
      assert value != null;
      if (value instanceof Double) {
        writeDouble(obj, (double) value);
      } else {
        assert value != Nil.nilObject;
        TruffleCompiler.transferToInterpreterAndInvalidate("unstabelized write node");
        obj.setUninitializedField(fieldIndex, value);
      }
    }

    @Override
    public void writeDouble(final SObject obj, final double value) {
      final long[] arr = obj.getExtendedPrimFields();
      unsafe.putDouble(arr,
          (long) Unsafe.ARRAY_DOUBLE_BASE_OFFSET
              + Unsafe.ARRAY_DOUBLE_INDEX_SCALE * this.extensionIndex,
          value);

      markAsSet(obj);
    }

    @Override
    public AbstractReadFieldNode getReadNode(final int fieldIndex,
        final ObjectLayout layout, final AbstractReadFieldNode next) {
      CompilerAsserts.neverPartOfCompilation("StorageLocation");
      return new ReadDoubleFieldNode(fieldIndex, layout, next);
    }

    @Override
    public AbstractWriteFieldNode getWriteNode(final int fieldIndex,
        final ObjectLayout layout, final AbstractWriteFieldNode next) {
      CompilerAsserts.neverPartOfCompilation("StorageLocation");
      return new WriteDoubleFieldNode(fieldIndex, layout, next);
    }
  }
}
