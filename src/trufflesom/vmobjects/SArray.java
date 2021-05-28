package trufflesom.vmobjects;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;

import trufflesom.vm.Universe;
import trufflesom.vm.constants.Nil;


/**
 * SArrays are implemented using a Strategy-like approach.
 * The SArray objects are 'tagged' with a type, and the strategy behavior
 * is implemented directly in the AST nodes.
 *
 * @author smarr
 */
public final class SArray extends SAbstractObject {
  public static final int FIRST_IDX = 0;

  public static SArray create(final Object[] values) {
    assert values.getClass() == Object[].class;
    return new SArray(values);
  }

  public static SArray create(final long[] values) {
    return new SArray(values);
  }

  public static SArray create(final double[] values) {
    return new SArray(values);
  }

  public static SArray create(final boolean[] values) {
    return new SArray(values);
  }

  public static SArray create(final int length) {
    return new SArray(length);
  }

  private Object storage;

  public int getEmptyStorage() {
    assert isEmptyType();
    return (int) storage;
  }

  public PartiallyEmptyArray getPartiallyEmptyStorage() {
    assert isPartiallyEmptyType();
    return (PartiallyEmptyArray) storage;
  }

  public Object[] getObjectStorage() {
    assert isObjectType();
    return CompilerDirectives.castExact(storage, Object[].class);
  }

  public long[] getLongStorage() {
    assert isLongType();
    return (long[]) storage;
  }

  public double[] getDoubleStorage() {
    assert isDoubleType();
    return (double[]) storage;
  }

  public boolean[] getBooleanStorage() {
    assert isBooleanType();
    return (boolean[]) storage;
  }

  public boolean isEmptyType() {
    return storage instanceof Integer;
  }

  public boolean isPartiallyEmptyType() {
    return storage instanceof PartiallyEmptyArray;
  }

  public boolean isObjectType() {
    return storage.getClass() == Object[].class;
  }

  public boolean isLongType() {
    return storage.getClass() == long[].class;
  }

  public boolean isDoubleType() {
    return storage.getClass() == double[].class;
  }

  public boolean isBooleanType() {
    return storage.getClass() == boolean[].class;
  }

  public boolean isSomePrimitiveType() {
    return isLongType() || isDoubleType() || isBooleanType();
  }

  /**
   * Creates and empty array, using the EMPTY strategy.
   *
   * @param length
   */
  public SArray(final long length) {
    storage = (int) length;
  }

  public SArray(final Object storage) {
    this.storage = storage;
  }

  private void fromEmptyToParticalWithType(final PartiallyEmptyArray.Type type, final long idx,
      final Object val) {
    assert type != PartiallyEmptyArray.Type.OBJECT;
    assert isEmptyType();
    int length = (int) storage;
    storage = new PartiallyEmptyArray(type, length, idx, val);
  }

  /**
   * Transition from the Empty, to the PartiallyEmpty state/strategy.
   */
  public void transitionFromEmptyToPartiallyEmptyWith(final long idx, final long val) {
    fromEmptyToParticalWithType(PartiallyEmptyArray.Type.LONG, idx, val);
  }

  public void transitionFromEmptyToPartiallyEmptyWith(final long idx, final double val) {
    fromEmptyToParticalWithType(PartiallyEmptyArray.Type.DOUBLE, idx, val);
  }

  public void transitionFromEmptyToPartiallyEmptyWith(final long idx, final boolean val) {
    fromEmptyToParticalWithType(PartiallyEmptyArray.Type.BOOLEAN, idx, val);
  }

  public void transitionToEmpty(final long length) {
    storage = (int) length;
  }

  public void transitionTo(final Object newStorage) {
    storage = newStorage;
  }

  public void transitionToObjectWithAll(final long length, final Object val) {
    Object[] arr = new Object[(int) length];
    Arrays.fill(arr, val);
    storage = arr;
  }

  public void transitionToLongWithAll(final long length, final long val) {
    long[] arr = new long[(int) length];
    Arrays.fill(arr, val);
    storage = arr;
  }

  public void transitionToDoubleWithAll(final long length, final double val) {
    double[] arr = new double[(int) length];
    Arrays.fill(arr, val);
    storage = arr;
  }

  public void transitionToBooleanWithAll(final long length, final boolean val) {
    boolean[] arr = new boolean[(int) length];
    if (val) {
      Arrays.fill(arr, true);
    }
    storage = arr;
  }

  private static long[] createLong(final Object[] arr) {
    long[] storage = new long[arr.length];
    for (int i = 0; i < arr.length; i++) {
      storage[i] = (long) arr[i];
    }
    return storage;
  }

  private static double[] createDouble(final Object[] arr) {
    double[] storage = new double[arr.length];
    for (int i = 0; i < arr.length; i++) {
      storage[i] = (double) arr[i];
    }
    return storage;
  }

  private static boolean[] createBoolean(final Object[] arr) {
    boolean[] storage = new boolean[arr.length];
    for (int i = 0; i < arr.length; i++) {
      storage[i] = (boolean) arr[i];
    }
    return storage;
  }

  public void ifFullOrObjectTransitionPartiallyEmpty() {
    PartiallyEmptyArray arr = getPartiallyEmptyStorage();

    if (arr.isFull()) {
      if (arr.getType() == PartiallyEmptyArray.Type.LONG) {
        storage = createLong(arr.getStorage());
      } else if (arr.getType() == PartiallyEmptyArray.Type.DOUBLE) {
        storage = createDouble(arr.getStorage());
      } else if (arr.getType() == PartiallyEmptyArray.Type.BOOLEAN) {
        storage = createBoolean(arr.getStorage());
      } else {
        storage = arr.getStorage();
      }
      return;
    }
    if (arr.getType() == PartiallyEmptyArray.Type.OBJECT) {
      storage = arr.getStorage();
    }
  }

  public static final class PartiallyEmptyArray {
    private final Object[] arr;
    private int            emptyElements;
    private Type           type;

    public enum Type {
      EMPTY, PARTIAL_EMPTY, LONG, DOUBLE, BOOLEAN, OBJECT;
    }

    public PartiallyEmptyArray(final Type type, final int length,
        final long idx, final Object val) {
      // can't specialize this here already,
      // because keeping track for nils would be to expensive
      arr = new Object[length];
      Arrays.fill(arr, Nil.nilObject);
      emptyElements = length - 1;
      arr[(int) idx] = val;
      this.type = type;
    }

    private PartiallyEmptyArray(final PartiallyEmptyArray old) {
      arr = old.arr.clone();
      emptyElements = old.emptyElements;
      type = old.type;
    }

    public Type getType() {
      return type;
    }

    public Object[] getStorage() {
      return arr;
    }

    public void setType(final Type type) {
      this.type = type;
    }

    public int getLength() {
      return arr.length;
    }

    public Object get(final long idx) {
      return arr[(int) idx];
    }

    public void set(final long idx, final Object val) {
      arr[(int) idx] = val;
    }

    public void incEmptyElements() {
      emptyElements++;
    }

    public void decEmptyElements() {
      emptyElements--;
    }

    public boolean isFull() {
      return emptyElements == 0;
    }

    public PartiallyEmptyArray copy() {
      return new PartiallyEmptyArray(this);
    }
  }

  /**
   * For internal use only, specifically, for SClass.
   * There we now, it is either empty, or of OBJECT type.
   *
   * @param value
   * @return
   */
  public SArray copyAndExtendWith(final Object value) {
    Object[] newArr;
    if (isEmptyType()) {
      newArr = new Object[] {value};
    } else {
      // if this is not true, this method is used in a wrong context
      assert isObjectType();
      Object[] s = getObjectStorage();
      newArr = Arrays.copyOf(s, s.length + 1);
      newArr[s.length] = value;
    }
    return new SArray(newArr);
  }

  @Override
  public SClass getSOMClass(final Universe universe) {
    return universe.arrayClass;
  }

  public Object debugGetObject(final int i) {
    return ((Object[]) storage)[i];
  }
}
