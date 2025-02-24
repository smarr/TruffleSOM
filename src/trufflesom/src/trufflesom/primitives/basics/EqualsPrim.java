package trufflesom.primitives.basics;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.bytecode.OperationProxy.Proxyable;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import trufflesom.bdt.primitives.Primitive;
import trufflesom.interpreter.Method.OpBuilder;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.nary.BinaryExpressionNode;
import trufflesom.interpreter.nodes.nary.BinaryMsgExprNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


@Proxyable
@Primitive(className = "Integer", primitive = "=")
@Primitive(className = "Double", primitive = "=")
@Primitive(className = "String", primitive = "=")
@Primitive(selector = "=")
@GenerateNodeFactory
@ImportStatic(SymbolTable.class)
public abstract class EqualsPrim extends BinaryExpressionNode {

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

  @Specialization
  public static final boolean doClasses(final SClass left, final SClass right) {
    return left == right;
  }

  @Fallback
  public static final Object genericSend(final VirtualFrame frame,
      final Object receiver, final Object argument,
      @Bind Node self,
      @Cached("create(symEquals)") final AbstractDispatchNode dispatch) {
    return dispatch.executeDispatch(frame, new Object[] {receiver, argument});
  }

  @Override
  public void constructOperation(final OpBuilder opBuilder, boolean resultUsed) {
    opBuilder.dsl.beginEqualsPrim();
    getReceiver().accept(opBuilder);
    getArgument().accept(opBuilder);
    opBuilder.dsl.endEqualsPrim();
  }
}
