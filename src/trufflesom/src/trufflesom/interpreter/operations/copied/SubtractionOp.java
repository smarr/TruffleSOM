package trufflesom.interpreter.operations.copied;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.bytecode.OperationProxy.Proxyable;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.primitives.arithmetic.ArithmeticPrim;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


@Proxyable
@ImportStatic(SymbolTable.class)
public abstract class SubtractionOp extends ArithmeticPrim {

  @Override
  public final SSymbol getSelector() {
    return SymbolTable.symMinus;
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

  @Specialization
  public static final Object doCached(final VirtualFrame frame, final SObject rcvr,
      final Object argument,
      @Cached("create(symMinus)") final AbstractDispatchNode dispatch) {
    return dispatch.executeDispatch(frame, new Object[] {rcvr, argument});
  }
}
