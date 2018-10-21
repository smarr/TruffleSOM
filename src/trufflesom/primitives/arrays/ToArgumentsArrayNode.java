package trufflesom.primitives.arrays;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ValueProfile;

import trufflesom.interpreter.SArguments;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SArray.ArrayType;


@GenerateNodeFactory
@ImportStatic(ArrayType.class)
@NodeChildren({
    @NodeChild("somArray"),
    @NodeChild("receiver")})
public abstract class ToArgumentsArrayNode extends ExpressionNode {

  private final ValueProfile storageType = ValueProfile.createClassProfile();

  public ToArgumentsArrayNode() {
    super(null);
  }

  public static final boolean isNull(final Object somArray) {
    return somArray == null;
  }

  public abstract Object[] executedEvaluated(SArray somArray, Object rcvr);

  public final Object[] executedEvaluated(final Object somArray, final Object rcvr) {
    return executedEvaluated((SArray) somArray, rcvr);
  }

  @Specialization(guards = "isNull(somArray)")
  public final Object[] doNoArray(final Object somArray, final Object rcvr) {
    return new Object[] {rcvr};
  }

  @Specialization(guards = "isEmptyType(somArray)")
  public final Object[] doEmptyArray(final SArray somArray, final Object rcvr) {
    Object[] result = new Object[somArray.getEmptyStorage(storageType) + 1];
    Arrays.fill(result, Nil.nilObject);
    result[SArguments.RCVR_IDX] = rcvr;
    return result;
  }

  private Object[] addRcvrToObjectArray(final Object rcvr, final Object[] storage) {
    Object[] argsArray = new Object[storage.length + 1];
    argsArray[SArguments.RCVR_IDX] = rcvr;
    System.arraycopy(storage, 0, argsArray, 1, storage.length);
    return argsArray;
  }

  @Specialization(guards = "isPartiallyEmptyType(somArray)")
  public final Object[] doPartiallyEmptyArray(final SArray somArray,
      final Object rcvr) {
    return addRcvrToObjectArray(
        rcvr, somArray.getPartiallyEmptyStorage(storageType).getStorage());
  }

  @Specialization(guards = "isObjectType(somArray)")
  public final Object[] doObjectArray(final SArray somArray,
      final Object rcvr) {
    return addRcvrToObjectArray(rcvr, somArray.getObjectStorage(storageType));
  }

  @Specialization(guards = "isLongType(somArray)")
  public final Object[] doLongArray(final SArray somArray,
      final Object rcvr) {
    long[] arr = somArray.getLongStorage(storageType);
    Object[] args = new Object[arr.length + 1];
    args[0] = rcvr;
    for (int i = 0; i < arr.length; i++) {
      args[i + 1] = arr[i];
    }
    return args;
  }

  @Specialization(guards = "isDoubleType(somArray)")
  public final Object[] doDoubleArray(final SArray somArray,
      final Object rcvr) {
    double[] arr = somArray.getDoubleStorage(storageType);
    Object[] args = new Object[arr.length + 1];
    args[0] = rcvr;
    for (int i = 0; i < arr.length; i++) {
      args[i + 1] = arr[i];
    }
    return args;
  }

  @Specialization(guards = "isBooleanType(somArray)")
  public final Object[] doBooleanArray(final SArray somArray,
      final Object rcvr) {
    boolean[] arr = somArray.getBooleanStorage(storageType);
    Object[] args = new Object[arr.length + 1];
    args[0] = rcvr;
    for (int i = 0; i < arr.length; i++) {
      args[i + 1] = arr[i];
    }
    return args;
  }
}
