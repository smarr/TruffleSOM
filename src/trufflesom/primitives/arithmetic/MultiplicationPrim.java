package trufflesom.primitives.arithmetic;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import bdt.primitives.Primitive;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SSymbol;


@GenerateNodeFactory
@Primitive(className = "Integer", primitive = "*")
@Primitive(className = "Double", primitive = "*")
@Primitive(selector = "*")
public abstract class MultiplicationPrim extends ArithmeticPrim {
  @Override
  public SSymbol getSelector() {
    return SymbolTable.symbolFor("*");
  }

  @Specialization(rewriteOn = ArithmeticException.class)
  public final long doLong(final long left, final long right) {
    return Math.multiplyExact(left, right);
  }

  @Specialization
  @TruffleBoundary
  public final Object doLongWithOverflow(final long left, final long right) {
    return reduceToLongIfPossible(
        BigInteger.valueOf(left).multiply(BigInteger.valueOf(right)));
  }

  @Specialization
  public final double doLong(final long left, final double right) {
    return doDouble(left, right);
  }

  @Specialization
  @TruffleBoundary
  public final Object doLong(final long left, final BigInteger right) {
    return doBigInteger(BigInteger.valueOf(left), right);
  }

  @Specialization
  @TruffleBoundary
  public final Object doBigInteger(final BigInteger left, final long right) {
    return doBigInteger(left, BigInteger.valueOf(right));
  }

  @Specialization
  @TruffleBoundary
  public final Object doDouble(final BigInteger left, final double right) {
    return left.doubleValue() * right;
  }

  @Specialization
  @TruffleBoundary
  public final Object doBigInteger(final BigInteger left, final BigInteger right) {
    BigInteger result = left.multiply(right);
    return reduceToLongIfPossible(result);
  }

  @Specialization
  public final double doDouble(final double left, final double right) {
    return left * right;
  }

  @Specialization
  public final double doDouble(final double left, final long right) {
    return left * right;
  }

  @Specialization
  @TruffleBoundary
  public final Object doDouble(final double left, final BigInteger right) {
    return left * right.doubleValue();
  }
}
