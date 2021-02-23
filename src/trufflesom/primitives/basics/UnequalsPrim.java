package trufflesom.primitives.basics;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import bd.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.BinaryExpressionNode.BinarySystemOperation;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


@Primitive(className = "Integer", primitive = "<>")
@Primitive(className = "Integer", primitive = "~=")
@Primitive(className = "Double", primitive = "<>")
@Primitive(className = "Double", primitive = "~=")
@Primitive(selector = "<>")
@Primitive(selector = "~=")
public abstract class UnequalsPrim extends BinarySystemOperation {

  @Specialization
  public final boolean doBoolean(final boolean left, final boolean right) {
    return left != right;
  }

  @Specialization
  public final boolean doBoolean(final boolean left, final SObject right) {
    return true;
  }

  @Specialization
  public final boolean doLong(final long left, final long right) {
    return left != right;
  }

  @Specialization
  public final boolean doBigInteger(final BigInteger left, final BigInteger right) {
    return left.compareTo(right) != 0;
  }

  @Specialization
  public final boolean doString(final String receiver, final String argument) {
    return !receiver.equals(argument);
  }

  @Specialization
  public final boolean doDouble(final double left, final double right) {
    return left != right;
  }

  @Specialization
  public final boolean doSSymbol(final SSymbol left, final SSymbol right) {
    return left != right;
  }

  @Specialization
  public final boolean doLong(final long left, final double right) {
    return left != right;
  }

  @Specialization
  public final boolean doBigInteger(final BigInteger left, final long right) {
    return doBigInteger(left, BigInteger.valueOf(right));
  }

  @Specialization
  public final boolean doLong(final long left, final BigInteger right) {
    return doBigInteger(BigInteger.valueOf(left), right);
  }

  @Specialization
  public final boolean doDouble(final double left, final long right) {
    return doDouble(left, (double) right);
  }

  @Specialization
  public final boolean doLong(final long left, final String right) {
    return true;
  }

  @Specialization
  public final boolean doLong(final long left, final SObject right) {
    return true;
  }

  @Specialization
  public final boolean doLong(final long left, final SSymbol right) {
    return true;
  }

  @Specialization
  public final boolean doString(final String receiver, final long argument) {
    return true;
  }

  @Specialization
  public final boolean doString(final String receiver, final SObject argument) {
    return true;
  }

  @Specialization
  public final boolean doSSymbol(final SSymbol receiver, final long argument) {
    return true;
  }

  @Specialization
  public final boolean doSSymbol(final SSymbol receiver, final SObject argument) {
    return true;
  }
}
