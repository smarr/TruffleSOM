package trufflesom.primitives.basics;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import trufflesom.bdt.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.BinaryMsgExprNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


@Primitive(className = "Integer", primitive = "=")
@Primitive(className = "Double", primitive = "=")
@Primitive(className = "String", primitive = "=")
@Primitive(selector = "=")
@GenerateNodeFactory
public abstract class EqualsPrim extends BinaryMsgExprNode {
  @Override
  public final SSymbol getSelector() {
    return SymbolTable.symbolFor("=");
  }

  @Specialization
  public static final boolean doBoolean(final boolean left, final boolean right) {
    return left == right;
  }

  @Specialization
  @SuppressWarnings("unused")
  public static final boolean doBoolean(final boolean left, final SObject right) {
    return false;
  }

  @Specialization
  public static final boolean doLong(final long left, final long right) {
    return left == right;
  }

  @Specialization
  public static final boolean doLong(final long left, final double right) {
    return left == right;
  }

  @Specialization
  @TruffleBoundary
  public static final boolean doLong(final long left, final BigInteger right) {
    return BigInteger.valueOf(left).compareTo(right) == 0;
  }

  @Specialization
  @SuppressWarnings("unused")
  public static final boolean doLong(final long left, final String right) {
    return false;
  }

  @Specialization
  @SuppressWarnings("unused")
  public static final boolean doLong(final long left, final SObject right) {
    return false;
  }

  @Specialization
  @SuppressWarnings("unused")
  public static final boolean doLong(final long left, final SSymbol right) {
    return false;
  }

  @Specialization
  @SuppressWarnings("unused")
  public static final boolean doBigInt(final BigInteger left, final String right) {
    return false;
  }

  @Specialization
  @TruffleBoundary
  public static final boolean doBigInteger(final BigInteger left, final long right) {
    return left.compareTo(BigInteger.valueOf(right)) == 0;
  }

  @Specialization
  @TruffleBoundary
  public static final boolean doBigInteger(final BigInteger left, final BigInteger right) {
    return left.compareTo(right) == 0;
  }

  @Specialization
  public static final boolean doDouble(final double left, final double right) {
    return left == right;
  }

  @Specialization
  public static final boolean doDouble(final double left, final long right) {
    return left == right;
  }

  @Specialization
  @TruffleBoundary
  public static final boolean doDouble(final double left, final BigInteger right) {
    return left == right.doubleValue();
  }

  @Specialization
  public static final boolean doString(final String receiver, final String argument) {
    return receiver.equals(argument);
  }

  @Specialization
  public static final boolean doString(final String receiver, final SSymbol argument) {
    return receiver.equals(argument.getString());
  }

  @Specialization
  @SuppressWarnings("unused")
  public static final boolean doString(final String receiver, final long argument) {
    return false;
  }

  @Specialization
  @SuppressWarnings("unused")
  public static final boolean doString(final String receiver, final SObject argument) {
    return false;
  }

  @Specialization
  public static final boolean doSSymbol(final SSymbol left, final SSymbol right) {
    return left == right;
  }

  @Specialization
  public static final boolean doSSymbol(final SSymbol receiver, final String argument) {
    return receiver.getString().equals(argument);
  }

  @Specialization
  @SuppressWarnings("unused")
  public static final boolean doSSymbol(final SSymbol receiver, final long argument) {
    return false;
  }

  @Specialization
  @SuppressWarnings("unused")
  public static final boolean doSSymbol(final SSymbol receiver, final SObject argument) {
    return false;
  }
}
