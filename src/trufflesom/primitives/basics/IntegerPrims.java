package trufflesom.primitives.basics;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;

import bd.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.BinaryExpressionNode;
import trufflesom.interpreter.nodes.nary.BinaryExpressionNode.BinarySystemOperation;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.primitives.arithmetic.ArithmeticPrim;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SSymbol;


public abstract class IntegerPrims {

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "atRandom")
  public abstract static class RandomPrim extends UnaryExpressionNode {
    @Specialization
    public final long doLong(final long receiver) {
      return (long) (receiver * Math.random());
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "as32BitSignedValue")
  public abstract static class As32BitSignedValue extends UnaryExpressionNode {
    @Specialization
    public final long doLong(final long receiver) {
      return (int) receiver;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "as32BitUnsignedValue")
  public abstract static class As32BitUnsignedValue extends UnaryExpressionNode {
    @Specialization
    public final long doLong(final long receiver) {
      return Integer.toUnsignedLong((int) receiver);
    }
  }

  @Primitive(className = "Integer", primitive = "fromString:", classSide = true)
  public abstract static class FromStringPrim extends BinarySystemOperation {
    @CompilationFinal protected SClass integerClass;

    @Override
    public BinarySystemOperation initialize(final Universe universe) {
      this.integerClass = universe.integerClass;
      return this;
    }

    @Specialization(guards = "receiver == integerClass")
    public final Object doSClass(final SClass receiver, final String argument) {
      return Long.parseLong(argument);
    }

    @Specialization(guards = "receiver == integerClass")
    public final Object doSClass(final SClass receiver, final SSymbol argument) {
      return Long.parseLong(argument.getString());
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "<<", selector = "<<")
  public abstract static class LeftShiftPrim extends ArithmeticPrim {
    private final BranchProfile overflow = BranchProfile.create();

    @Specialization(rewriteOn = ArithmeticException.class)
    public final long doLong(final long receiver, final long right) {
      assert right >= 0; // currently not defined for negative values of right

      if (Long.SIZE - Long.numberOfLeadingZeros(receiver) + right > Long.SIZE - 1) {
        overflow.enter();
        throw new ArithmeticException("shift overflows long");
      }
      return receiver << right;
    }

    @Specialization
    public final BigInteger doLongWithOverflow(final long receiver, final long right) {
      assert right >= 0; // currently not defined for negative values of right
      assert right <= Integer.MAX_VALUE;

      return BigInteger.valueOf(receiver).shiftLeft((int) right);
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = ">>>", selector = ">>>")
  public abstract static class UnsignedRightShiftPrim extends ArithmeticPrim {
    @Specialization
    public final long doLong(final long receiver, final long right) {
      return receiver >>> right;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "max:")
  @Primitive(selector = "max:")
  public abstract static class MaxIntPrim extends ArithmeticPrim {
    @Specialization
    public final long doLong(final long receiver, final long right) {
      return Math.max(receiver, right);
    }

    @Specialization
    public BigInteger doLongBig(final long left, final BigInteger right) {
      return BigInteger.valueOf(left).max(right);
    }

    @Specialization
    public BigInteger doBigLong(final BigInteger left, final long right) {
      return left.max(BigInteger.valueOf(right));
    }

    @Specialization
    public BigInteger doBig(final BigInteger left, final BigInteger right) {
      return left.max(right);
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "to:", selector = "to:",
      receiverType = Long.class, disabled = true)
  public abstract static class ToPrim extends BinaryExpressionNode {
    @Specialization
    public final SArray doLong(final long receiver, final long right) {
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
      receiverType = Long.class)
  public abstract static class AbsPrim extends UnaryExpressionNode {
    @Specialization
    public final long doLong(final long receiver) {
      return Math.abs(receiver);
    }
  }
}
