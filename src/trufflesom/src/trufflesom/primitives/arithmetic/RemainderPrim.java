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
@Primitive(className = "Integer", primitive = "rem:", selector = "rem:")
public abstract class RemainderPrim extends ArithmeticPrim {
  @Override
  public final SSymbol getSelector() {
    return SymbolTable.symbolFor("rem:");
  }

  @Specialization
  public static final double doDouble(final double left, final double right) {
    return left % right;
  }

  @Specialization
  public static final double doDouble(final double left, final long right) {
    return doDouble(left, (double) right);
  }

  @Specialization
  @TruffleBoundary
  public static final Object doBigInteger(final BigInteger left, final BigInteger right) {
    return reduceToLongIfPossible(left.remainder(right));
  }

  @Specialization
  @TruffleBoundary
  public static final Object doBigInteger(final BigInteger left, final long right) {
    return doBigInteger(left, BigInteger.valueOf(right));
  }

  @Specialization
  @TruffleBoundary
  public static final Object doLong(final long left, final BigInteger right) {
    return doBigInteger(BigInteger.valueOf(left), right);
  }

  @Specialization
  public static final double doLong(final long left, final double right) {
    return doDouble(left, right);
  }

  @Specialization(rewriteOn = ArithmeticException.class)
  public static final long doLong(final long left, final long right) {
    return left % right;
  }

  @Override
  public void constructOperation(final OpBuilder opBuilder, boolean resultUsed) {
    opBuilder.dsl.beginRemainderPrim();
    getReceiver().accept(opBuilder);
    getArgument().accept(opBuilder);
    opBuilder.dsl.endRemainderPrim();
  }
}
