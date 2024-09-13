package trufflesom.primitives.basics;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.bytecode.OperationProxy.Proxyable;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import trufflesom.bdt.primitives.Primitive;
import trufflesom.interpreter.Method.OpBuilder;
import trufflesom.interpreter.nodes.nary.BinaryMsgExprNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


@Proxyable
@Primitive(className = "Integer", primitive = "<>")
@Primitive(className = "Double", primitive = "<>")
@Primitive(selector = "<>")
@GenerateNodeFactory
public abstract class UnequalsPrim extends BinaryMsgExprNode {
  @Override
  public final SSymbol getSelector() {
    return SymbolTable.symbolFor("<>");
  }

  @Specialization
  public static final boolean doBoolean(final boolean left, final boolean right) {
    return left != right;
  }

  @Specialization
  @SuppressWarnings("unused")
  public static final boolean doBoolean(final boolean left, final SObject right) {
    return true;
  }

  @Specialization
  public static final boolean doLong(final long left, final long right) {
    return left != right;
  }

  @Specialization
  @TruffleBoundary
  public static final boolean doBigInteger(final BigInteger left, final BigInteger right) {
    return left.compareTo(right) != 0;
  }

  @Specialization
  public static final boolean doString(final String receiver, final String argument) {
    return !receiver.equals(argument);
  }

  @Specialization
  public static final boolean doDouble(final double left, final double right) {
    return left != right;
  }

  @Specialization
  public static final boolean doSSymbol(final SSymbol left, final SSymbol right) {
    return left != right;
  }

  @Specialization
  public static final boolean doLong(final long left, final double right) {
    return left != right;
  }

  @Specialization
  @TruffleBoundary
  public static final boolean doBigInteger(final BigInteger left, final long right) {
    return doBigInteger(left, BigInteger.valueOf(right));
  }

  @Specialization
  @TruffleBoundary
  public static final boolean doLong(final long left, final BigInteger right) {
    return doBigInteger(BigInteger.valueOf(left), right);
  }

  @Specialization
  public static final boolean doDouble(final double left, final long right) {
    return doDouble(left, (double) right);
  }

  @Specialization
  @SuppressWarnings("unused")
  public static final boolean doLong(final long left, final String right) {
    return true;
  }

  @Specialization
  @SuppressWarnings("unused")
  public static final boolean doLong(final long left, final SObject right) {
    return true;
  }

  @Specialization
  @SuppressWarnings("unused")
  public static final boolean doLong(final long left, final SSymbol right) {
    return true;
  }

  @Specialization
  @SuppressWarnings("unused")
  public static final boolean doString(final String receiver, final long argument) {
    return true;
  }

  @Specialization
  @SuppressWarnings("unused")
  public static final boolean doString(final String receiver, final SObject argument) {
    return true;
  }

  @Specialization
  @SuppressWarnings("unused")
  public static final boolean doSSymbol(final SSymbol receiver, final long argument) {
    return true;
  }

  @Specialization
  @SuppressWarnings("unused")
  public static final boolean doSSymbol(final SSymbol receiver, final SObject argument) {
    return true;
  }

  @Override
  public void constructOperation(final OpBuilder opBuilder, boolean resultUsed) {
    opBuilder.dsl.beginUnequalsPrim();
    getReceiver().accept(opBuilder);
    getArgument().accept(opBuilder);
    opBuilder.dsl.endUnequalsPrim();
  }
}
