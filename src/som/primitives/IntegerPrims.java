package som.primitives;

import java.math.BigInteger;

import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.primitives.arithmetic.ArithmeticPrim;
import som.vm.constants.Classes;
import som.vmobjects.SClass;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.dsl.Specialization;

public abstract class IntegerPrims {

  public abstract static class RandomPrim extends UnaryExpressionNode {
    @Specialization
    public final long doLong(final long receiver) {
      return (long) (receiver * Math.random());
    }
  }

  public abstract static class As32BitSignedValue extends UnaryExpressionNode {
    @Specialization
    public final long doLong(final long receiver) {
      return (int) receiver;
    }
  }

  public abstract static class As32BitUnsignedValue extends UnaryExpressionNode {
    @Specialization
    public final long doLong(final long receiver) {
      return Integer.toUnsignedLong((int) receiver);
    }
  }

  public abstract static class FromStringPrim extends ArithmeticPrim {
    protected final boolean receiverIsIntegerClass(final SClass receiver) {
      return receiver == Classes.integerClass;
    }

    @Specialization(guards = "receiverIsIntegerClass")
    public final Object doSClass(final SClass receiver, final String argument) {
      return Long.parseLong(argument);
    }

    @Specialization(guards = "receiverIsIntegerClass")
    public final Object doSClass(final SClass receiver, final SSymbol argument) {
      return Long.parseLong(argument.getString());
    }
  }

  public abstract static class LeftShiftPrim extends ArithmeticPrim {
    @Specialization(rewriteOn = ArithmeticException.class)
    public final long doLong(final long receiver, final long right) {
      assert right >= 0;  // currently not defined for negative values of right

      if (Long.SIZE - Long.numberOfLeadingZeros(receiver) + right > Long.SIZE - 1) {
        throw new ArithmeticException("shift overflows long");
      }
      return receiver << right;
    }

    @Specialization
    public final Object doLongWithOverflow(final long receiver, final long right) {
      assert right >= 0;  // currently not defined for negative values of right
      assert right <= Integer.MAX_VALUE;

      BigInteger result = BigInteger.valueOf(receiver).shiftLeft((int) right);
      try {
        return result.longValueExact();
      } catch (ArithmeticException e) {
        return result;
      }
    }
  }

  public abstract static class UnsignedRightShiftPrim extends ArithmeticPrim {
    @Specialization
    public final long doLong(final long receiver, final long right) {
      return receiver >>> right;
    }
  }
}
