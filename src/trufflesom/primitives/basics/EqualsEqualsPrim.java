package trufflesom.primitives.basics;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import bd.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.BinaryMsgExprNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


@GenerateNodeFactory
@Primitive(className = "Object", primitive = "==", selector = "==")
public abstract class EqualsEqualsPrim extends BinaryMsgExprNode {
  @Override
  public SSymbol getSelector() {
    return SymbolTable.symbolFor("==");
  }

  @Specialization
  public final boolean doBoolean(final boolean left, final boolean right) {
    return left == right;
  }

  @Specialization
  public final boolean doBoolean(final boolean left, final SObject right) {
    return false;
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
  public final boolean doString(final String receiver, final SSymbol argument) {
    return false;
  }

  @Specialization
  public final boolean doString(final String receiver, final SObject argument) {
    return false;
  }
}
