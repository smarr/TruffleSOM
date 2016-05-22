package som.primitives;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.constants.Classes;
import som.vmobjects.SClass;


public abstract class DoublePrims  {

  @GenerateNodeFactory
  public abstract static class RoundPrim extends UnaryExpressionNode {
    @Specialization
    public final long doDouble(final double receiver) {
      return Math.round(receiver);
    }
  }

  @GenerateNodeFactory
  public abstract static class AsIntegerPrim extends UnaryExpressionNode {
    @Specialization
    public final long doDouble(final double receiver) {
      return (long) receiver;
    }
  }

  @GenerateNodeFactory
  public abstract static class PositiveInfinityPrim extends UnaryExpressionNode {
    protected final boolean receiverIsDoubleClass(final SClass receiver) {
      return receiver == Classes.doubleClass;
    }

    @Specialization(guards = "receiverIsDoubleClass(receiver)")
    public final double doSClass(final SClass receiver) {
      return Double.POSITIVE_INFINITY;
    }
  }
}
