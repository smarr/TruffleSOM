package trufflesom.primitives.arithmetic;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import bd.primitives.Primitive;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SSymbol;


@GenerateNodeFactory
@Primitive(className = "Integer", primitive = "+")
@Primitive(className = "Double", primitive = "+")
@Primitive(selector = "+")
public abstract class AdditionPrim extends ArithmeticPrim {
  @Specialization(rewriteOn = ArithmeticException.class)
  public final long doLong(final long left, final long argument) {
    return Math.addExact(left, argument);
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
  public final Object doDouble(final BigInteger left, final double right) {
    return left.doubleValue() + right;
  }

  @Specialization
  public final Object doDouble(final double left, final BigInteger right) {
    return left + right.doubleValue();
  }

  @Specialization
  public final double doDouble(final double left, final long right) {
    return left + right;
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
