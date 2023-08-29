package trufflesom.primitives.arrays;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import trufflesom.bdt.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.vmobjects.SArray;


@GenerateNodeFactory
@Primitive(className = "Array", primitive = "copy")
public abstract class CopyPrim extends UnaryExpressionNode {
  @Specialization(guards = "receiver.isEmptyType()")
  public static final SArray doEmptyArray(final SArray receiver) {
    return new SArray(receiver.getEmptyStorage());
  }

  @Specialization(guards = "receiver.isPartiallyEmptyType()")
  public static final SArray doPartiallyEmptyArray(final SArray receiver) {
    return new SArray(
        receiver.getPartiallyEmptyStorage().copy());
  }

  @Specialization(guards = "receiver.isObjectType()")
  public static final SArray doObjectArray(final SArray receiver) {
    return SArray.create(receiver.getObjectStorage().clone());
  }

  @Specialization(guards = "receiver.isLongType()")
  public static final SArray doLongArray(final SArray receiver) {
    return SArray.create(receiver.getLongStorage().clone());
  }

  @Specialization(guards = "receiver.isDoubleType()")
  public static final SArray doDoubleArray(final SArray receiver) {
    return SArray.create(receiver.getDoubleStorage().clone());
  }

  @Specialization(guards = "receiver.isBooleanType()")
  public static final SArray doBooleanArray(final SArray receiver) {
    return SArray.create(receiver.getBooleanStorage().clone());
  }
}
