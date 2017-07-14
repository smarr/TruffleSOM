package som.primitives.arithmetic;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;

import som.primitives.Primitive;


@GenerateNodeFactory
@Primitive(className = "Integer", primitive = "&", selector = "&")
public abstract class LogicAndPrim extends ArithmeticPrim {
  public LogicAndPrim(final SourceSection source) {
    super(source);
  }

  @Specialization
  public final long doLong(final long left, final long right) {
    return left & right;
  }

  @Specialization
  public final Object doBigInteger(final BigInteger left, final BigInteger right) {
    return reduceToLongIfPossible(left.and(right));
  }

  @Specialization
  public final Object doLong(final long left, final BigInteger right) {
    return doBigInteger(BigInteger.valueOf(left), right);
  }

  @Specialization
  public final Object doBigInteger(final BigInteger left, final long right) {
    return doBigInteger(left, BigInteger.valueOf(right));
  }
}
