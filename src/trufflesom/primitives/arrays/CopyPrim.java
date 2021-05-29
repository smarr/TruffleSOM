package trufflesom.primitives.arrays;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import bd.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.vmobjects.SArray;


@GenerateNodeFactory
@Primitive(className = "Array", primitive = "copy")
public abstract class CopyPrim extends UnaryExpressionNode {
  @Specialization(guards = "receiver.isEmptyType()")
  public final SArray doEmptyArray(final SArray receiver) {
    return new SArray(receiver.getEmptyStorage());
  }

  @Specialization(guards = "receiver.isPartiallyEmptyType()")
  public final SArray doPartiallyEmptyArray(final SArray receiver) {
    return new SArray(
        receiver.getPartiallyEmptyStorage().copy());
  }

  @Specialization(guards = "receiver.isObjectType()")
  public final SArray doObjectArray(final SArray receiver) {
    return SArray.create(receiver.getObjectStorage().clone());
  }

  @Specialization(guards = "receiver.isLongType()")
  public final SArray doLongArray(final SArray receiver) {
    return SArray.create(receiver.getLongStorage().clone());
  }

  @Specialization(guards = "receiver.isDoubleType()")
  public final SArray doDoubleArray(final SArray receiver) {
    return SArray.create(receiver.getDoubleStorage().clone());
  }

  @Specialization(guards = "receiver.isBooleanType()")
  public final SArray doBooleanArray(final SArray receiver) {
    return SArray.create(receiver.getBooleanStorage().clone());
  }
}
