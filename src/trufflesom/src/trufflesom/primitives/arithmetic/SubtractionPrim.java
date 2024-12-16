package trufflesom.primitives.arithmetic;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import trufflesom.bdt.primitives.Primitive;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SSymbol;


@GenerateNodeFactory
@Primitive(className = "Integer", primitive = "-")
@Primitive(className = "Double", primitive = "-")
@Primitive(selector = "-")
public abstract class SubtractionPrim extends ArithmeticPrim {
  @Override
  public final SSymbol getSelector() {
    return SymbolTable.symbolFor("-");
  }

  @Specialization(rewriteOn = ArithmeticException.class)
  public static final long doLong(final long left, final long right) {
    return Math.subtractExact(left, right);
  }

  @Specialization
  @TruffleBoundary
  public static final Object doLongWithOverflow(final long left, final long right) {
    return reduceToLongIfPossible(
        BigInteger.valueOf(left).subtract(BigInteger.valueOf(right)));
  }

  @Specialization
  @TruffleBoundary
  public static final Object doLong(final long left, final BigInteger right) {
    return doBigInteger(BigInteger.valueOf(left), right);
  }

  @Specialization
  public static final double doLong(final long left, final double right) {
    return left - right;
  }

  @Specialization
  @TruffleBoundary
  public static final Object doBigInteger(final BigInteger left, final long right) {
    return doBigInteger(left, BigInteger.valueOf(right));
  }

  @Specialization
  @TruffleBoundary
  public static final Object doDouble(final BigInteger left, final double right) {
    return left.doubleValue() - right;
  }

  @Specialization
  @TruffleBoundary
  public static final Object doBigInteger(final BigInteger left, final BigInteger right) {
    BigInteger result = left.subtract(right);
    return reduceToLongIfPossible(result);
  }

  @Specialization
  public static final double doDouble(final double left, final double right) {
    return left - right;
  }

  @Specialization
  public static final double doDouble(final double left, final long right) {
    return left - right;
  }

  @Specialization
  @TruffleBoundary
  public static final Object doDouble(final double left, final BigInteger right) {
    return left - right.doubleValue();
  }
}
