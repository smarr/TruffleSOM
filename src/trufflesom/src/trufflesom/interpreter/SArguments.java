package trufflesom.interpreter;

import trufflesom.vmobjects.SArray;


public final class SArguments {

  /**
   * Create a new array from an SArguments array that contains only the true
   * arguments and excludes the receiver. This is used for instance for
   * #doesNotUnderstand (#dnu)
   */
  public static SArray getArgumentsWithoutReceiver(final Object[] arguments) {
    // the code and magic numbers below are based on the following assumption
    assert arguments.length >= 1; // <- that's the receiver
    Object[] argsArr = new Object[arguments.length - 1];
    if (argsArr.length == 0) {
      return SArray.create(0);
    }
    System.arraycopy(arguments, 1, argsArr, 0, argsArr.length);
    return SArray.create(argsArr);
  }
}
