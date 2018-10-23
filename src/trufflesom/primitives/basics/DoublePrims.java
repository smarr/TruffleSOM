package trufflesom.primitives.basics;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import bd.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode.UnarySystemOperation;
import trufflesom.vmobjects.SClass;


public abstract class DoublePrims {

  @GenerateNodeFactory
  @Primitive(className = "Double", primitive = "round")
  public abstract static class RoundPrim extends UnaryExpressionNode {
    @Specialization
    public final long doDouble(final double receiver) {
      return Math.round(receiver);
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Double", primitive = "asInteger")
  public abstract static class AsIntegerPrim extends UnaryExpressionNode {
    @Specialization
    public final long doDouble(final double receiver) {
      return (long) receiver;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Double", primitive = "PositiveInfinity", classSide = true)
  public abstract static class PositiveInfinityPrim extends UnarySystemOperation {
    @Specialization(guards = "receiver == universe.doubleClass")
    public final double doSClass(final SClass receiver) {
      return Double.POSITIVE_INFINITY;
    }
  }
}
