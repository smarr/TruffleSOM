package som.primitives.basics;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.vm.Universe;
import som.vmobjects.SArray;
import som.vmobjects.SBlock;
import som.vmobjects.SInvokable;
import som.vmobjects.SObject;
import som.vmobjects.SSymbol;


@GenerateNodeFactory
public abstract class EqualsEqualsPrim extends BinaryExpressionNode {

  private final SObject trueObject;
  private final SObject falseObject;

  public EqualsEqualsPrim(final Universe universe) {
    this.trueObject = universe.getTrueObject();
    this.falseObject = universe.getFalseObject();
  }

  @Specialization
  public final boolean doBoolean(final boolean left, final boolean right) {
    return left == right;
  }

  @Specialization
  public final boolean doBoolean(final boolean left, final SObject right) {
    return (left && trueObject == right) ||
        (!left && falseObject == right);
  }

  @Specialization
  public final boolean doLong(final long left, final long right) {
    return left == right;
  }

  @Specialization
  public final boolean doBigInteger(final BigInteger left, final BigInteger right) {
    return left == right;
  }

  @Specialization
  public final boolean doString(final String left, final String right) {
    return left == right;
  }

  @Specialization
  public final boolean doDouble(final double left, final double right) {
    return left == right;
  }

  @Specialization
  public final boolean doSBlock(final SBlock left, final Object right) {
    return left == right;
  }

  @Specialization
  public final boolean doArray(final SArray left, final Object right) {
    return left == right;
  }

  @Specialization
  public final boolean doSMethod(final SInvokable left, final Object right) {
    return left == right;
  }

  @Specialization
  public final boolean doSSymbol(final SSymbol left, final Object right) {
    return left == right;
  }

  @Specialization
  public final boolean doSObject(final SObject left, final Object right) {
    return left == right;
  }

  @Specialization
  public final boolean doLong(final long left, final double right) {
    return false;
  }

  @Specialization
  public final boolean doBigInteger(final BigInteger left, final long right) {
    return false;
  }

  @Specialization
  public final boolean doLong(final long left, final BigInteger right) {
    return false;
  }

  @Specialization
  public final boolean doDouble(final double left, final long right) {
    return false;
  }

  @Specialization
  public final boolean doLong(final long left, final String right) {
    return false;
  }

  @Specialization
  public final boolean doLong(final long left, final SObject right) {
    return false;
  }

  @Specialization
  public final boolean doString(final String receiver, final long argument) {
    return false;
  }

  @Specialization
  public final boolean doString(final String receiver, final SObject argument) {
    return false;
  }
}
