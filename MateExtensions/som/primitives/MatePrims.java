package som.primitives;

import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.MateUniverse;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SClass;
import som.vmobjects.SObject;
import som.vmobjects.SShape;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

public final class MatePrims {
  @GenerateNodeFactory
  public abstract static class MateNewObjectPrim extends UnaryExpressionNode {
    @Specialization
    public final SAbstractObject doSClass(final SClass receiver) {
      return MateUniverse.newInstance(receiver);
    }
  }

  @GenerateNodeFactory
  public abstract static class MateNewEnvironmentPrim extends UnaryExpressionNode {
    @Specialization
    public final SAbstractObject doSClass(final SClass receiver) {
      return MateUniverse.newEnvironment(receiver);
    }
  }
  
  @GenerateNodeFactory
  public abstract static class MateNewShapePrim extends BinaryExpressionNode {
    @Specialization
    public final SAbstractObject doSClass(final SClass receiver, final long fieldsCount) {
      return new SShape((int)fieldsCount);
    }
  }
  
  @GenerateNodeFactory
  public abstract static class MateChangeShapePrim extends BinaryExpressionNode {
    @Specialization
    public final SAbstractObject doSObject(final SObject receiver, SShape newShape) {
      //receiver.
      return receiver;
    }
  }
}

