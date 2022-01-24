package trufflesom.primitives.arrays;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;

import trufflesom.interpreter.nodes.NoPreEvalExprNode;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SArray;


@GenerateNodeFactory
@NodeChildren({
    @NodeChild("somArray"),
    @NodeChild("receiver")})
public abstract class ToArgumentsArrayNode extends NoPreEvalExprNode {

  public abstract Object[] executedEvaluated(SArray somArray, Object rcvr);

  public final Object[] executedEvaluated(final Object somArray, final Object rcvr) {
    return executedEvaluated((SArray) somArray, rcvr);
  }

  @Specialization(guards = "somArray == null")
  public final Object[] doNoArray(final Object somArray, final Object rcvr) {
    return new Object[] {rcvr};
  }

  @Specialization(guards = "somArray.isEmptyType()")
  public final Object[] doEmptyArray(final SArray somArray, final Object rcvr) {
    Object[] result = new Object[somArray.getEmptyStorage() + 1];
    Arrays.fill(result, Nil.nilObject);
    result[0] = rcvr;
    return result;
  }

  private Object[] addRcvrToObjectArray(final Object rcvr, final Object[] storage) {
    Object[] argsArray = new Object[storage.length + 1];
    argsArray[0] = rcvr;
    System.arraycopy(storage, 0, argsArray, 1, storage.length);
    return argsArray;
  }

  @Specialization(guards = "somArray.isPartiallyEmptyType()")
  public final Object[] doPartiallyEmptyArray(final SArray somArray,
      final Object rcvr) {
    return addRcvrToObjectArray(
        rcvr, somArray.getPartiallyEmptyStorage().getStorage());
  }

  @Specialization(guards = "somArray.isObjectType()")
  public final Object[] doObjectArray(final SArray somArray,
      final Object rcvr) {
    return addRcvrToObjectArray(rcvr, somArray.getObjectStorage());
  }

  @Specialization(guards = "somArray.isLongType()")
  public final Object[] doLongArray(final SArray somArray,
      final Object rcvr) {
    long[] arr = somArray.getLongStorage();
    Object[] args = new Object[arr.length + 1];
    args[0] = rcvr;
    for (int i = 0; i < arr.length; i++) {
      args[i + 1] = arr[i];
    }
    return args;
  }

  @Specialization(guards = "somArray.isDoubleType()")
  public final Object[] doDoubleArray(final SArray somArray,
      final Object rcvr) {
    double[] arr = somArray.getDoubleStorage();
    Object[] args = new Object[arr.length + 1];
    args[0] = rcvr;
    for (int i = 0; i < arr.length; i++) {
      args[i + 1] = arr[i];
    }
    return args;
  }

  @Specialization(guards = "somArray.isBooleanType()")
  public final Object[] doBooleanArray(final SArray somArray,
      final Object rcvr) {
    boolean[] arr = somArray.getBooleanStorage();
    Object[] args = new Object[arr.length + 1];
    args[0] = rcvr;
    for (int i = 0; i < arr.length; i++) {
      args[i + 1] = arr[i];
    }
    return args;
  }
}
