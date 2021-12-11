package trufflesom.primitives.basics;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import bd.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.BinaryExpressionNode;


@GenerateNodeFactory
@Primitive(className = "Object", primitive = "==", selector = "==")
public abstract class EqualsEqualsPrim extends BinaryExpressionNode {
  @Specialization
  public final boolean doLong(final long left, final long right) {
    return left == right;
  }

  @Specialization
  public final boolean doDouble(final double left, final double right) {
    return left == right;
  }

  @Fallback
  public final boolean fallback(final Object receiver, final Object argument) {
    return receiver == argument;
  }
}
