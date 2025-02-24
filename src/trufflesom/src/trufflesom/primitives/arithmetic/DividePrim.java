package trufflesom.primitives.arithmetic;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.bytecode.OperationProxy.Proxyable;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import trufflesom.bdt.primitives.Primitive;
import trufflesom.interpreter.Method.OpBuilder;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SSymbol;


@Proxyable
@GenerateNodeFactory
@Primitive(className = "Integer", primitive = "/", selector = "/")
public abstract class DividePrim extends ArithmeticPrim {
  @Override
  public final SSymbol getSelector() {
    return SymbolTable.symbolFor("/");
  }

  @Specialization
  public static final long doLong(final long left, final long right) {
    return left / right;
  }

  @Specialization
  public static final long doLong(final long left, final double right) {
    return (long) (left / right);
  }

  @Specialization
  @TruffleBoundary
  public static final Object doLong(final long left, final BigInteger right) {
    return doBigInteger(BigInteger.valueOf(left), right);
  }

  @Specialization
  @TruffleBoundary
  public static final Object doBigInteger(final BigInteger left, final BigInteger right) {
    BigInteger result = left.divide(right);
    return reduceToLongIfPossible(result);
  }

  @Specialization
  @TruffleBoundary
  public static final Object doBigInteger(final BigInteger left, final long right) {
    return doBigInteger(left, BigInteger.valueOf(right));
  }

  @Override
  public void constructOperation(final OpBuilder opBuilder) {
    opBuilder.dsl.beginDividePrim();
    getReceiver().accept(opBuilder);
    getArgument().accept(opBuilder);
    opBuilder.dsl.endDividePrim();
  }
}
