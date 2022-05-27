package trufflesom.primitives.basics;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.ProbeNode;

import bdt.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SSymbol;


@GenerateNodeFactory
@GenerateWrapper
@Primitive(className = "Array", primitive = "length")
@Primitive(className = "String", primitive = "length")
@Primitive(selector = "length", receiverType = {String.class, SArray.class, SSymbol.class},
    inParser = false)
public abstract class LengthPrim extends UnaryExpressionNode {

  @Specialization(guards = "receiver.isEmptyType()")
  public final long doEmptySArray(final SArray receiver) {
    return receiver.getEmptyStorage();
  }

  @Specialization(guards = "receiver.isPartiallyEmptyType()")
  public final long doPartialEmptySArray(final SArray receiver) {
    return receiver.getPartiallyEmptyStorage().getLength();
  }

  @Specialization(guards = "receiver.isObjectType()")
  public final long doObjectSArray(final SArray receiver) {
    return receiver.getObjectStorage().length;
  }

  @Specialization(guards = "receiver.isLongType()")
  public final long doLongSArray(final SArray receiver) {
    return receiver.getLongStorage().length;
  }

  @Specialization(guards = "receiver.isDoubleType()")
  public final long doDoubleSArray(final SArray receiver) {
    return receiver.getDoubleStorage().length;
  }

  @Specialization(guards = "receiver.isBooleanType()")
  public final long doBooleanSArray(final SArray receiver) {
    return receiver.getBooleanStorage().length;
  }

  public abstract long executeEvaluated(VirtualFrame frame, SArray receiver);

  @Specialization
  public final long doString(final String receiver) {
    return receiver.length();
  }

  @Specialization
  public final long doSSymbol(final SSymbol receiver) {
    return receiver.getString().length();
  }

  @Override
  public WrapperNode createWrapper(final ProbeNode probe) {
    return new LengthPrimWrapper(this, probe);
  }
}
