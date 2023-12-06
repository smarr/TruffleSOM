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
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


@Proxyable
@ImportStatic(SymbolTable.class)
public abstract class AdditionOp extends ArithmeticPrim {

  @Override
  public final SSymbol getSelector() {
    return SymbolTable.symPlus;
  }

  @Specialization(rewriteOn = ArithmeticException.class)
  public static final long doLong(final long left, final long argument) {
    return Math.addExact(left, argument);
  }

  @Specialization
  @TruffleBoundary
  public static final Object doLongWithOverflow(final long left, final long argument) {
    return reduceToLongIfPossible(BigInteger.valueOf(left).add(BigInteger.valueOf(argument)));
  }

  @Specialization
  @TruffleBoundary
  public static final Object doLong(final long left, final BigInteger argument) {
    return doBigInteger(BigInteger.valueOf(left), argument);
  }

  @Specialization
  public static final double doLong(final long left, final double argument) {
    return doDouble(left, argument);
  }

  @Specialization
  @TruffleBoundary
  public static final Object doBigInteger(final BigInteger left, final BigInteger right) {
    BigInteger result = left.add(right);
    return reduceToLongIfPossible(result);
  }

  @Specialization
  @TruffleBoundary
  public static final Object doBigInteger(final BigInteger left, final long right) {
    return doBigInteger(left, BigInteger.valueOf(right));
  }

  @Specialization
  @TruffleBoundary
  public static final Object doDouble(final BigInteger left, final double right) {
    return left.doubleValue() + right;
  }

  @Specialization
  public static final double doDouble(final double left, final double right) {
    return right + left;
  }

  @Specialization
  @TruffleBoundary
  public static final Object doDouble(final double left, final BigInteger right) {
    return left + right.doubleValue();
  }

  @Specialization
  public static final double doDouble(final double left, final long right) {
    return left + right;
  }

  @Specialization
  @TruffleBoundary
  public static final String doString(final String left, final String right) {
    return left + right;
  }

  @Specialization
  @TruffleBoundary
  public static final String doString(final String left, final long right) {
    return left + right;
  }

  @Specialization
  @TruffleBoundary
  public static final String doString(final String left, final SClass right) {
    return left + right.getName().getString();
  }

  @Specialization
  @TruffleBoundary
  public static final String doString(final String left, final SSymbol right) {
    return left + right.getString();
  }

  @Specialization
  @TruffleBoundary
  public static final SSymbol doSSymbol(final SSymbol left, final SSymbol right) {
    return SymbolTable.symbolFor(left.getString() + right.getString());
  }

  @Specialization
  @TruffleBoundary
  public static final SSymbol doSSymbol(final SSymbol left, final String right) {
    return SymbolTable.symbolFor(left.getString() + right);
  }

  @Specialization
  public static final Object doCached(final VirtualFrame frame, final SObject rcvr,
      final Object argument,
      @Cached("create(symPlus)") final AbstractDispatchNode dispatch) {
    return dispatch.executeDispatch(frame, new Object[] {rcvr, argument});
  }
}
