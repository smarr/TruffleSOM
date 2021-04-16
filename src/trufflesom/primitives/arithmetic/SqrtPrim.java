package trufflesom.primitives.arithmetic;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;

import bd.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;


@GenerateNodeFactory
@Primitive(className = "Integer", primitive = "sqrt")
@Primitive(className = "Double", primitive = "sqrt")
public abstract class SqrtPrim extends UnaryExpressionNode {

  private final BranchProfile longReturn   = BranchProfile.create();
  private final BranchProfile doubleReturn = BranchProfile.create();

  @Specialization
  public final Object doLong(final long receiver) {
    double result = Math.sqrt(receiver);

    if (result == Math.rint(result)) {
      longReturn.enter();
      return (long) result;
    } else {
      doubleReturn.enter();
      return result;
    }
  }

  @Specialization
  @TruffleBoundary
  public final double doBigInteger(final BigInteger receiver) {
    return Math.sqrt(receiver.doubleValue());
  }

  @Specialization
  public final double doDouble(final double receiver) {
    return Math.sqrt(receiver);
  }
}
