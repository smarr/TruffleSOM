package trufflesom.primitives.basics;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;

import trufflesom.bdt.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.BinaryExpressionNode;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.vm.Classes;
import trufflesom.vmobjects.SClass;


public abstract class DoublePrims {

  @GenerateNodeFactory
  @Primitive(className = "Double", primitive = "round")
  public abstract static class RoundPrim extends UnaryExpressionNode {
    @Specialization
    public static final long doDouble(final double receiver) {
      return Math.round(receiver);
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Double", primitive = "asInteger")
  public abstract static class AsIntegerPrim extends UnaryExpressionNode {
    @Specialization
    public static final long doDouble(final double receiver) {
      return (long) receiver;
    }
  }

  @ImportStatic(Classes.class)
  @GenerateNodeFactory
  @Primitive(className = "Double", primitive = "PositiveInfinity", classSide = true)
  public abstract static class PositiveInfinityPrim extends UnaryExpressionNode {
    @Specialization(guards = "receiver == doubleClass")
    public static final double doSClass(final SClass receiver) {
      return Double.POSITIVE_INFINITY;
    }
  }

  @ImportStatic(Classes.class)
  @GenerateNodeFactory
  @Primitive(className = "Double", primitive = "fromString:", classSide = true)
  public abstract static class FromStringPrim extends BinaryExpressionNode {

    @TruffleBoundary
    @Specialization(guards = "receiver == doubleClass")
    public static final double doSClass(final SClass receiver, final String str) {
      try {
        return Double.parseDouble(str);
      } catch (NumberFormatException e) {
        return Double.NaN;
      }
    }
  }
}
