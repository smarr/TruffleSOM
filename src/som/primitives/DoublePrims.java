package som.primitives;

import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.constants.Classes;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;


public abstract class DoublePrims  {

  @GenerateNodeFactory
  public abstract static class RoundPrim extends UnaryExpressionNode {
    @Specialization
    public final long doDouble(final double receiver) {
      return Math.round(receiver);
    }
  }

  @GenerateNodeFactory
  public abstract static class PositiveInfinityPrim extends UnaryExpressionNode {
    protected final boolean receiverIsDoubleClass(final DynamicObject receiver) {
      return receiver == Classes.doubleClass;
    }

    @Specialization(guards = "receiverIsDoubleClass(receiver)")
    public final double doSClass(final DynamicObject receiver) {
      return Double.POSITIVE_INFINITY;
    }
  }
}
