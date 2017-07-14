package som.primitives.basics;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.SourceSection;

import bd.primitives.Primitive;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.primitives.arithmetic.ArithmeticPrim;
import som.vm.Universe;
import som.vmobjects.SArray;
import som.vmobjects.SClass;
import som.vmobjects.SSymbol;


public abstract class IntegerPrims {

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "atRandom")
  public abstract static class RandomPrim extends UnaryExpressionNode {
    public RandomPrim(final SourceSection source) {
      super(source);
    }

    @Specialization
    public final long doLong(final long receiver) {
      return (long) (receiver * Math.random());
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "as32BitSignedValue")
  public abstract static class As32BitSignedValue extends UnaryExpressionNode {
    public As32BitSignedValue(final SourceSection source) {
      super(source);
    }

    @Specialization
    public final long doLong(final long receiver) {
      return (int) receiver;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "as32BitUnsignedValue")
  public abstract static class As32BitUnsignedValue extends UnaryExpressionNode {
    public As32BitUnsignedValue(final SourceSection source) {
      super(source);
    }

    @Specialization
    public final long doLong(final long receiver) {
      return Integer.toUnsignedLong((int) receiver);
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "fromString:", classSide = true,
      requiresContext = true)
  public abstract static class FromStringPrim extends ArithmeticPrim {
    protected final SClass integerClass;

    public FromStringPrim(final SourceSection source, final Universe universe) {
      super(source);
      this.integerClass = universe.integerClass;
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

    public LeftShiftPrim(final SourceSection source) {
      super(source);
    }

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
    public UnsignedRightShiftPrim(final SourceSection source) {
      super(source);
    }

    @Specialization
    public final long doLong(final long receiver, final long right) {
      return receiver >>> right;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "max:")
  public abstract static class MaxIntPrim extends ArithmeticPrim {
    public MaxIntPrim(final SourceSection source) {
      super(source);
    }

    @Specialization
    public final long doLong(final long receiver, final long right) {
      return Math.max(receiver, right);
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "to:", selector = "to:",
      receiverType = Long.class, disabled = true)
  public abstract static class ToPrim extends BinaryExpressionNode {
    public ToPrim(final SourceSection source) {
      super(source);
    }

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
    public AbsPrim(final SourceSection source) {
      super(source);
    }

    @Specialization
    public final long doLong(final long receiver) {
      return Math.abs(receiver);
    }
  }
}
