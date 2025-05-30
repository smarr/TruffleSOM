package trufflesom.primitives.basics;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;

import trufflesom.bdt.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.BinaryExpressionNode;
import trufflesom.interpreter.nodes.nary.BinaryMsgExprNode;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.interpreter.nodes.nary.UnaryMsgExprNode;
import trufflesom.primitives.arithmetic.ArithmeticPrim;
import trufflesom.vm.Classes;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SSymbol;


public abstract class IntegerPrims {

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "atRandom")
  public abstract static class RandomPrim extends UnaryExpressionNode {
    @Specialization
    public static final long doLong(final long receiver) {
      return (long) (receiver * Math.random());
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "as32BitSignedValue")
  @Primitive(selector = "as32BitSignedValue")
  public abstract static class As32BitSignedValue extends UnaryExpressionNode {
    @Specialization
    public static final long doLong(final long receiver) {
      return (int) receiver;
    }

    @Specialization
    @TruffleBoundary
    public static final long doBig(final BigInteger receiver) {
      return receiver.intValue();
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "as32BitUnsignedValue")
  @Primitive(selector = "as32BitUnsignedValue")
  public abstract static class As32BitUnsignedValue extends UnaryExpressionNode {
    @Specialization
    public static final long doLong(final long receiver) {
      return Integer.toUnsignedLong((int) receiver);
    }

    @Specialization
    @TruffleBoundary
    public static final long doBig(final BigInteger receiver) {
      return Integer.toUnsignedLong(receiver.intValue());
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "asDouble")
  @Primitive(selector = "asDouble")
  public abstract static class AsDoubleValue extends UnaryExpressionNode {
    @Specialization
    public static final double doLong(final long receiver) {
      return receiver;
    }

    @Specialization
    @TruffleBoundary
    public static final double doBig(final BigInteger receiver) {
      return receiver.doubleValue();
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Double", primitive = "negated")
  @Primitive(className = "Integer", primitive = "negated")
  @Primitive(selector = "negated")
  public abstract static class NegatedValue extends UnaryExpressionNode {
    @Specialization
    public static final long doLong(final long receiver) {
      return -receiver;
    }

    @Specialization
    public static final double doDouble(final double receiver) {
      return -receiver;
    }

    @Specialization
    @TruffleBoundary
    public static final BigInteger doBig(final BigInteger receiver) {
      return receiver.negate();
    }
  }

  @ImportStatic(Classes.class)
  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "fromString:", classSide = true)
  public abstract static class FromStringPrim extends BinaryExpressionNode {

    @TruffleBoundary
    @Specialization(guards = "receiver == integerClass")
    public static final Object doString(@SuppressWarnings("unused") final SClass receiver,
        final String argument) {
      try {
        return Long.parseLong(argument);
      } catch (NumberFormatException e) {
        return new BigInteger(argument);
      }
    }

    @Specialization(guards = "receiver == integerClass")
    public static final Object doSymbol(final SClass receiver, final SSymbol argument) {
      return doString(receiver, argument.getString());
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "<<", selector = "<<")
  public abstract static class LeftShiftPrim extends ArithmeticPrim {

    @Override
    public final SSymbol getSelector() {
      return SymbolTable.symbolFor("<<");
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    public static final long doLong(final long receiver, final long right,
        @Cached final InlinedBranchProfile overflow, @Bind final Node node) {
      assert right >= 0; // currently not defined for negative values of right

      if (Long.SIZE - Long.numberOfLeadingZeros(receiver) + right > Long.SIZE - 1) {
        overflow.enter(node);
        throw new ArithmeticException("shift overflows long");
      }
      return receiver << right;
    }

    @Specialization
    @TruffleBoundary
    public static final BigInteger doLongWithOverflow(final long receiver, final long right) {
      assert right >= 0; // currently not defined for negative values of right
      assert right <= Integer.MAX_VALUE;

      return BigInteger.valueOf(receiver).shiftLeft((int) right);
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = ">>>", selector = ">>>")
  public abstract static class UnsignedRightShiftPrim extends ArithmeticPrim {
    @Override
    public final SSymbol getSelector() {
      return SymbolTable.symbolFor(">>>");
    }

    @Specialization
    public static final long doLong(final long receiver, final long right) {
      return receiver >>> right;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "min:")
  @Primitive(selector = "min:")
  public abstract static class MinIntPrim extends ArithmeticPrim {
    @Override
    public final SSymbol getSelector() {
      return SymbolTable.symbolFor("min:");
    }

    @Specialization
    public static final long doLong(final long receiver, final long right) {
      return Math.min(receiver, right);
    }

    @Specialization
    @TruffleBoundary
    public static BigInteger doLongBig(final long left, final BigInteger right) {
      return BigInteger.valueOf(left).min(right);
    }

    @Specialization
    @TruffleBoundary
    public static BigInteger doBigLong(final BigInteger left, final long right) {
      return left.min(BigInteger.valueOf(right));
    }

    @Specialization
    @TruffleBoundary
    public static BigInteger doBig(final BigInteger left, final BigInteger right) {
      return left.min(right);
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "max:")
  @Primitive(selector = "max:")
  public abstract static class MaxIntPrim extends ArithmeticPrim {
    @Override
    public final SSymbol getSelector() {
      return SymbolTable.symbolFor("max:");
    }

    @Specialization
    public static final long doLong(final long receiver, final long right) {
      return Math.max(receiver, right);
    }

    @Specialization
    @TruffleBoundary
    public static BigInteger doLongBig(final long left, final BigInteger right) {
      return BigInteger.valueOf(left).max(right);
    }

    @Specialization
    @TruffleBoundary
    public static BigInteger doBigLong(final BigInteger left, final long right) {
      return left.max(BigInteger.valueOf(right));
    }

    @Specialization
    @TruffleBoundary
    public static BigInteger doBig(final BigInteger left, final BigInteger right) {
      return left.max(right);
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "to:", selector = "to:",
      receiverType = Long.class, disabled = true)
  public abstract static class ToPrim extends BinaryMsgExprNode {
    @Override
    public final SSymbol getSelector() {
      return SymbolTable.symbolFor("to:");
    }

    @Specialization
    public static final SArray doLong(final long receiver, final long right) {
      int cnt = (int) right - (int) receiver + 1;
      long[] arr = new long[cnt];
      for (int i = 0; i < cnt; i++) {
        arr[i] = i + receiver;
      }
      return SArray.create(arr);
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "abs", selector = "abs",
      receiverType = {Long.class, BigInteger.class})
  @Primitive(className = "Double", primitive = "abs", receiverType = {Double.class})
  public abstract static class AbsPrim extends UnaryMsgExprNode {
    public static final boolean minLong(final long receiver) {
      return receiver == Long.MIN_VALUE;
    }

    /**
     * Math.abs(MIN_VALUE) == MIN_VALUE, which is wrong.
     */
    @Specialization(guards = "!minLong(receiver)")
    public static final long doLong(final long receiver) {
      return Math.abs(receiver);
    }

    @Specialization(guards = "minLong(receiver)")
    @TruffleBoundary
    public static final BigInteger doLongMinValue(
        @SuppressWarnings("unused") final long receiver) {
      return BigInteger.valueOf(Long.MIN_VALUE).abs();
    }

    @Specialization
    @TruffleBoundary
    public static final BigInteger doBig(final BigInteger receiver) {
      return receiver.abs();
    }

    @Specialization
    public static final double doDouble(final double receiver) {
      return Math.abs(receiver);
    }

    @Override
    public final SSymbol getSelector() {
      return SymbolTable.symbolFor("abs");
    }
  }
}
