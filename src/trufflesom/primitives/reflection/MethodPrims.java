package som.primitives.reflection;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.PreevaluatedExpression;
import som.interpreter.nodes.dispatch.InvokeOnCache;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.primitives.Primitive;
import som.primitives.arrays.ToArgumentsArrayNode;
import som.primitives.arrays.ToArgumentsArrayNodeFactory;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SArray;
import som.vmobjects.SInvokable;


public final class MethodPrims {

  @GenerateNodeFactory
  @Primitive(className = "Method", primitive = "signature")
  @Primitive(className = "Primitive", primitive = "signature")
  public abstract static class SignaturePrim extends UnaryExpressionNode {
    public SignaturePrim(final SourceSection source) {
      super(source);
    }

    @Specialization
    public final SAbstractObject doSMethod(final SInvokable receiver) {
      return receiver.getSignature();
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Method", primitive = "holder")
  @Primitive(className = "Primitive", primitive = "holder")
  public abstract static class HolderPrim extends UnaryExpressionNode {
    public HolderPrim(final SourceSection source) {
      super(source);
    }

    @Specialization
    public final SAbstractObject doSMethod(final SInvokable receiver) {
      return receiver.getHolder();
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Method", primitive = "invokeOn:with:",
      extraChild = ToArgumentsArrayNodeFactory.class)
  @Primitive(className = "Primitive", primitive = "invokeOn:with:",
      extraChild = ToArgumentsArrayNodeFactory.class)
  @NodeChildren({
      @NodeChild(value = "receiver", type = ExpressionNode.class),
      @NodeChild(value = "target", type = ExpressionNode.class),
      @NodeChild(value = "somArr", type = ExpressionNode.class),
      @NodeChild(value = "argArr", type = ToArgumentsArrayNode.class,
          executeWith = {"somArr", "target"})})
  @Primitive(selector = "invokeOn:with:", noWrapper = true,
      extraChild = ToArgumentsArrayNodeFactory.class)
  public abstract static class InvokeOnPrim extends ExpressionNode
      implements PreevaluatedExpression {
    @Child private InvokeOnCache callNode;

    public InvokeOnPrim(final SourceSection source) {
      super(source);
      callNode = InvokeOnCache.create();
    }

    public abstract Object executeEvaluated(VirtualFrame frame, SInvokable receiver,
        Object target, SArray somArr);

    @Override
    public final Object doPreEvaluated(final VirtualFrame frame,
        final Object[] args) {
      return executeEvaluated(frame, (SInvokable) args[0], args[1], (SArray) args[2]);
    }

    @Specialization
    public final Object doInvoke(final VirtualFrame frame,
        final SInvokable receiver, final Object target, final SArray somArr,
        final Object[] argArr) {
      return callNode.executeDispatch(frame, receiver, argArr);
    }
  }
}
