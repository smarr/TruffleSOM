package trufflesom.interpreter.objectstorage;

import java.lang.reflect.Field;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import sun.misc.Unsafe;
import trufflesom.vmobjects.SObject;


public class StorageAnalyzer {
  private static final Unsafe unsafe = UnsafeUtil.load();

  private static final long SMO_PRIM_FIELD_1_OFFSET = getFieldOffset("primField1");
  private static final long SMO_PRIM_FIELD_2_OFFSET = getFieldOffset("primField2");
  private static final long SMO_PRIM_FIELD_3_OFFSET = getFieldOffset("primField3");
  private static final long SMO_PRIM_FIELD_4_OFFSET = getFieldOffset("primField4");
  private static final long SMO_PRIM_FIELD_5_OFFSET = getFieldOffset("primField5");
  private static final long SMO_FIELD_1_OFFSET      = getFieldOffset("field1");
  private static final long SMO_FIELD_2_OFFSET      = getFieldOffset("field2");
  private static final long SMO_FIELD_3_OFFSET      = getFieldOffset("field3");
  private static final long SMO_FIELD_4_OFFSET      = getFieldOffset("field4");
  private static final long SMO_FIELD_5_OFFSET      = getFieldOffset("field5");

  @CompilationFinal(
      dimensions = 1) private static final DirectObjectAccessor[]                  objAccessors  =
          new DirectObjectAccessor[SObject.NUM_OBJECT_FIELDS];
  @CompilationFinal(
      dimensions = 1) private static final DirectPrimitiveAccessor[]               primAccessors =
          new DirectPrimitiveAccessor[SObject.NUM_PRIMITIVE_FIELDS];

  @SuppressWarnings("deprecation")
  private static long getFieldOffset(final String fieldName) {
    try {
      Field field = SObject.class.getDeclaredField(fieldName);
      return unsafe.objectFieldOffset(field);
    } catch (NoSuchFieldException | SecurityException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Initialize field accessors with the offsets in the S*Object classes.
   */
  @TruffleBoundary
  public static void initAccessors() {
    if (objAccessors[0] != null) {
      return;
    }

    initObjectAccessors();
    initPrimitiveAccessors();
  }

  private static void initObjectAccessors() {
    objAccessors[0] = new DirectObjectAccessor(SMO_FIELD_1_OFFSET);
    objAccessors[1] = new DirectObjectAccessor(SMO_FIELD_2_OFFSET);
    objAccessors[2] = new DirectObjectAccessor(SMO_FIELD_3_OFFSET);
    objAccessors[3] = new DirectObjectAccessor(SMO_FIELD_4_OFFSET);
    objAccessors[4] = new DirectObjectAccessor(SMO_FIELD_5_OFFSET);
  }

  private static void initPrimitiveAccessors() {
    primAccessors[0] = new DirectPrimitiveAccessor(SMO_PRIM_FIELD_1_OFFSET);
    primAccessors[1] = new DirectPrimitiveAccessor(SMO_PRIM_FIELD_2_OFFSET);
    primAccessors[2] = new DirectPrimitiveAccessor(SMO_PRIM_FIELD_3_OFFSET);
    primAccessors[3] = new DirectPrimitiveAccessor(SMO_PRIM_FIELD_4_OFFSET);
    primAccessors[4] = new DirectPrimitiveAccessor(SMO_PRIM_FIELD_5_OFFSET);
  }

  public static final class DirectObjectAccessor {
    private final long fieldOffset;

    private DirectObjectAccessor(final long fieldOffset) {
      this.fieldOffset = fieldOffset;
    }

    public long getFieldOffset() {
      return fieldOffset;
    }

    public Object read(final SObject obj) {
      return unsafe.getObject(obj, fieldOffset);
    }

    public void write(final SObject obj, final Object value) {
      assert value != null;
      unsafe.putObject(obj, fieldOffset, value);
    }
  }

  public static final class DirectPrimitiveAccessor {
    private final long offset;

    private DirectPrimitiveAccessor(final long fieldOffset) {
      offset = fieldOffset;
    }

    public long getFieldOffset() {
      return offset;
    }

    public long readLong(final SObject obj) {
      return unsafe.getLong(obj, offset);
    }

    public double readDouble(final SObject obj) {
      return unsafe.getDouble(obj, offset);
    }

    public void write(final SObject obj, final long value) {
      unsafe.putLong(obj, offset, value);
    }

    public void write(final SObject obj, final double value) {
      unsafe.putDouble(obj, offset, value);
    }
  }

  public static long getPrimitiveFieldOffset(final int primField) {
    return primAccessors[primField].getFieldOffset();
  }

  public static long getObjectFieldOffset(final int fieldIndex) {
    return objAccessors[fieldIndex].getFieldOffset();
  }
}
