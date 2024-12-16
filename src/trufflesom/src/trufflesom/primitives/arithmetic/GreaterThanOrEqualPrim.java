package trufflesom.primitives.arithmetic;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import trufflesom.bdt.primitives.Primitive;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SSymbol;


@GenerateNodeFactory
@Primitive(className = "Integer", primitive = ">=")
@Primitive(className = "Double", primitive = ">=")
@Primitive(selector = ">=")
public abstract class GreaterThanOrEqualPrim extends ArithmeticPrim {
  @Override
  public final SSymbol getSelector() {
    return SymbolTable.symbolFor(">=");
  }

  @Specialization
  public static final boolean doLong(final long left, final long right) {
    return left >= right;
  }

  @Specialization
  @TruffleBoundary
  public static final boolean doLong(final long left, final BigInteger right) {
    return doBigInteger(BigInteger.valueOf(left), right);
  }

  @Specialization
  public static final boolean doLong(final long left, final double right) {
    return doDouble(left, right);
  }

  @Specialization
  public static final boolean doDouble(final double left, final long right) {
    return doDouble(left, right);
  }

  @Specialization
  public static final boolean doDouble(final double left, final double right) {
    return left >= right;
  }

  @Specialization
  @TruffleBoundary
  public static final boolean doBigInteger(final BigInteger left, final BigInteger right) {
    return left.compareTo(right) >= 0;
  }

  @Specialization
  @TruffleBoundary
  public static final boolean doBigInteger(final BigInteger left, final long right) {
    return doBigInteger(left, BigInteger.valueOf(right));
  }
}
