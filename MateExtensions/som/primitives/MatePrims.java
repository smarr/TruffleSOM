package som.primitives;

import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.MateUniverse;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SShape;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.object.basic.DynamicObjectBasic;

public final class MatePrims {
  @GenerateNodeFactory
  public abstract static class MateNewEnvironmentPrim extends UnaryExpressionNode {
    @Specialization
    public final DynamicObjectBasic doSClass(final DynamicObjectBasic receiver) {
      return MateUniverse.newEnvironment(receiver);
    }
  }

  @GenerateNodeFactory
  public abstract static class MateNewShapePrim extends BinaryExpressionNode {
    @Specialization
    public final SAbstractObject doSClass(final DynamicObjectBasic receiver, final long fieldsCount) {
      return new SShape((int)fieldsCount);
    }
  }

  @GenerateNodeFactory
  public abstract static class MateChangeShapePrim extends BinaryExpressionNode {
    @Specialization
    public final DynamicObjectBasic doSObject(final DynamicObjectBasic receiver, final SShape newShape) {
      receiver.setShapeAndResize(receiver.getShape(), newShape.getShape());
      return receiver;
    }
  }

  @GenerateNodeFactory
  public abstract static class MateShapeFieldsCountPrim extends UnaryExpressionNode {
    @Specialization
    public final long doSShape(final SShape shape) {
      return shape.getShape().getPropertyCount();
    }
  }

  @GenerateNodeFactory
  public abstract static class MateGetShapePrim extends UnaryExpressionNode {
    @Specialization
    public final SShape doSObject(final DynamicObjectBasic receiver) {
      return new SShape(receiver.getShape().getPropertyCount());
    }
  }
}

