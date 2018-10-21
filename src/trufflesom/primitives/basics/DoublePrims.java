package trufflesom.primitives.basics;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.primitives.Primitive;
import trufflesom.primitives.basics.SystemPrims.UnarySystemNode;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SClass;


public abstract class DoublePrims {

  @GenerateNodeFactory
  @Primitive(className = "Double", primitive = "round")
  public abstract static class RoundPrim extends UnaryExpressionNode {
    public RoundPrim(final SourceSection source) {
      super(source);
    }

    @Specialization
    public final long doDouble(final double receiver) {
      return Math.round(receiver);
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Double", primitive = "asInteger")
  public abstract static class AsIntegerPrim extends UnaryExpressionNode {
    public AsIntegerPrim(final SourceSection source) {
      super(source);
    }

    @Specialization
    public final long doDouble(final double receiver) {
      return (long) receiver;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Double", primitive = "PositiveInfinity", classSide = true,
      requiresContext = true)
  public abstract static class PositiveInfinityPrim extends UnarySystemNode {
    public PositiveInfinityPrim(final SourceSection source, final Universe universe) {
      super(source, universe);
    }

    @Specialization(guards = "receiver == universe.doubleClass")
    public final double doSClass(final SClass receiver) {
      return Double.POSITIVE_INFINITY;
    }
  }
}
