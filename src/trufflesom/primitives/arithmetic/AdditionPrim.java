package som.primitives.arithmetic;

import java.math.BigInteger;

import com.oracle.truffle.api.ExactMath;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;

import som.primitives.Primitive;
import som.vmobjects.SClass;
import som.vmobjects.SSymbol;


@GenerateNodeFactory
@Primitive(className = "Integer", primitive = "+")
@Primitive(className = "Double", primitive = "+")
@Primitive(selector = "+")
public abstract class AdditionPrim extends ArithmeticPrim {
  public AdditionPrim(final SourceSection source) {
    super(source);
  }

  @Specialization(rewriteOn = ArithmeticException.class)
  public final long doLong(final long left, final long argument) {
    return ExactMath.addExact(left, argument);
  }

  @Specialization
  public final BigInteger doLongWithOverflow(final long left, final long argument) {
    return BigInteger.valueOf(left).add(BigInteger.valueOf(argument));
  }

  @Specialization
  public final Object doBigInteger(final BigInteger left, final BigInteger right) {
    BigInteger result = left.add(right);
    return reduceToLongIfPossible(result);
  }

  @Specialization
  public final double doDouble(final double left, final double right) {
    return right + left;
  }

  @Specialization
  public final String doString(final String left, final String right) {
    return left + right;
  }

  @Specialization
  public final String doSSymbol(final SSymbol left, final SSymbol right) {
    return left.getString() + right.getString();
  }

  @Specialization
  public final String doSSymbol(final SSymbol left, final String right) {
    return left.getString() + right;
  }

  @Specialization
  public final Object doLong(final long left, final BigInteger argument) {
    return doBigInteger(BigInteger.valueOf(left), argument);
  }

  @Specialization
  public final double doLong(final long left, final double argument) {
    return doDouble(left, argument);
  }

  @Specialization
  public final Object doBigInteger(final BigInteger left, final long right) {
    return doBigInteger(left, BigInteger.valueOf(right));
  }

  @Specialization
  public final double doDouble(final double left, final long right) {
    return doDouble(left, (double) right);
  }

  @Specialization
  public final String doString(final String left, final SClass right) {
    return left + right.getName().getString();
  }

  @Specialization
  public final String doString(final String left, final SSymbol right) {
    return left + right.getString();
  }
}
