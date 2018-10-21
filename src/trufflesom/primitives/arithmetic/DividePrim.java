package som.primitives.arithmetic;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;

import som.primitives.Primitive;
import som.vm.NotYetImplementedException;


@GenerateNodeFactory
@Primitive(className = "Integer", primitive = "/", selector = "/")
public abstract class DividePrim extends ArithmeticPrim {
  public DividePrim(final SourceSection source) {
    super(source);
  }

  @Specialization
  public final long doLong(final long left, final long right) {
    return left / right;
  }

  @Specialization
  public final Object doBigInteger(final BigInteger left, final BigInteger right) {
    BigInteger result = left.divide(right);
    return reduceToLongIfPossible(result);
  }

  @Specialization
  public final Object doBigInteger(final BigInteger left, final long right) {
    return doBigInteger(left, BigInteger.valueOf(right));
  }

  @Specialization
  public final Object doLong(final long left, final BigInteger right) {
    return doBigInteger(BigInteger.valueOf(left), right);
  }

  @Specialization
  public final Object doLong(final long left, final double right) {
    CompilerAsserts.neverPartOfCompilation("DividePrim");
    throw new NotYetImplementedException(); // TODO: need to implement the "//" case here
                                            // directly... : resendAsDouble("//", left,
                                            // (SDouble) rightObj, frame.pack());
  }
}
