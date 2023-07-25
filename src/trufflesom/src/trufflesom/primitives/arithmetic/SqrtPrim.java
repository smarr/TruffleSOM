package trufflesom.primitives.arithmetic;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import bdt.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;


@GenerateNodeFactory
@Primitive(className = "Integer", primitive = "sqrt")
@Primitive(className = "Double", primitive = "sqrt")
public abstract class SqrtPrim extends UnaryExpressionNode {

  @Specialization
  public static final Object doLong(final long receiver,
      @Cached final InlinedConditionProfile longOrDouble, @Bind("this") final Node node) {
    double result = Math.sqrt(receiver);

    if (longOrDouble.profile(node, result == Math.rint(result))) {
      return (long) result;
    } else {
      return result;
    }
  }

  @Specialization
  @TruffleBoundary
  public static final double doBigInteger(final BigInteger receiver) {
    return Math.sqrt(receiver.doubleValue());
  }

  @Specialization
  public static final double doDouble(final double receiver) {
    return Math.sqrt(receiver);
  }
}
