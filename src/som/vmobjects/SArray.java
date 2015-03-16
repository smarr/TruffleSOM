package som.vmobjects;

import java.util.Arrays;

import som.vm.constants.Nil;

/**
 * SArrays are implemented using a Strategy-like approach.
 * The SArray objects are 'tagged' with a type, and the strategy behavior
 * is implemented directly in the AST nodes.
 *
 * @author smarr
 */
public abstract class SArray {
  public static final int FIRST_IDX = 0;

  public static Object[] newSArray(final long length) {
    Object[] result = new Object[(int) length];
    Arrays.fill(result, Nil.nilObject);
    return result;
  }

  public static Object get(final Object[] arr, final long idx) {
    return arr[(int) idx - 1];
  }

  public static void set(final Object[] arr, final long idx, final Object val) {
    arr[(int) idx - 1] = val;
  }
}
