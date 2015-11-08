package som.interpreter;

import som.vm.constants.ExecutionLevel;
import som.vmobjects.SArray;
import som.vmobjects.SMateEnvironment;

import com.oracle.truffle.api.frame.Frame;

public final class SArguments {

  private static final int ENVIRONMENT_IDX = 0;
  private static final int EXECUTION_LEVEL_IDX = 1;
  public static final int RCVR_IDX = 2;
  
  private static final int ARGUMENT_OFFSET = RCVR_IDX;

  private static Object[] args(final Frame frame) {
    return frame.getArguments();
  }

  public static Object arg(final Frame frame, final int index) {
    return args(frame)[index + ARGUMENT_OFFSET];
  }

  public static Object rcvr(final Frame frame) {
    return args(frame)[RCVR_IDX];
  }
  
  public static SMateEnvironment getEnvironment(final Frame frame) {
    return (SMateEnvironment)args(frame)[ENVIRONMENT_IDX];
  }
  
  public static ExecutionLevel getExecutionLevel(final Frame frame) {
    return (ExecutionLevel)args(frame)[EXECUTION_LEVEL_IDX];
  }

  /**
   * Create a new array from an SArguments array that contains only the true
   * arguments and excludes the receiver. This is used for instance for
   * #doesNotUnderstand (#dnu)
   */
  public static SArray getArgumentsWithoutReceiver(final Object[] arguments) {
    // the code and magic numbers below are based on the following assumption
    //assert RCVR_IDX == 0;
    assert arguments.length >= 1;  // <- that's the receiver
    Object[] argsArr = new Object[arguments.length - 1];
    if (argsArr.length == 0) {
      return SArray.create(0);
    }
    System.arraycopy(arguments, 1, argsArr, 0, argsArr.length);
    return SArray.create(argsArr);
  }
  
  public static Object[] createSArguments(final SMateEnvironment environment,
      final ExecutionLevel exLevel, final Object[] arguments) {
    Object[] args = new Object[arguments.length + ARGUMENT_OFFSET];
    args[ENVIRONMENT_IDX]     = environment;
    args[EXECUTION_LEVEL_IDX] = exLevel;
    System.arraycopy(arguments, 0, args, ARGUMENT_OFFSET, arguments.length);
    return args;
  }
}
