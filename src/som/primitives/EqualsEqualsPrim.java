package som.primitives;

import java.math.BigInteger;

import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.vm.constants.Globals;
import som.vmobjects.SArray;
import som.vmobjects.SBlock;
import som.vmobjects.SInvokable;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;


@GenerateNodeFactory
public abstract class EqualsEqualsPrim extends BinaryExpressionNode {

  @Specialization
  public final boolean doBoolean(final boolean left, final boolean right) {
    return left == right;
  }

  @Specialization
  public final boolean doBoolean(final boolean left, final DynamicObject right) {
    return (left && Globals.trueObject  == right) ||
          (!left && Globals.falseObject == right);
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
  public final boolean doSObject(final DynamicObject left, final Object right) {
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
  public final boolean doLong(final long left, final DynamicObject right) {
    return false;
  }

  @Specialization
  public final boolean doString(final String receiver, final long argument) {
    return false;
  }

  @Specialization
  public final boolean doString(final String receiver, final DynamicObject argument) {
    return false;
  }
}
