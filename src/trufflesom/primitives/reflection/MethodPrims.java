package trufflesom.primitives.reflection;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import bd.primitives.Primitive;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.dispatch.InvokeOnCache;
import trufflesom.interpreter.nodes.nary.EagerlySpecializableNode;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.primitives.arrays.ToArgumentsArrayNode;
import trufflesom.primitives.arrays.ToArgumentsArrayNodeFactory;
import trufflesom.vm.NotYetImplementedException;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SAbstractObject;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SSymbol;


public final class MethodPrims {

  @GenerateNodeFactory
  @Primitive(className = "Method", primitive = "signature")
  @Primitive(className = "Primitive", primitive = "signature")
  public abstract static class SignaturePrim extends UnaryExpressionNode {
    @Specialization
    public final SAbstractObject doSMethod(final SInvokable receiver) {
      return receiver.getSignature();
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Method", primitive = "holder")
  @Primitive(className = "Primitive", primitive = "holder")
  public abstract static class HolderPrim extends UnaryExpressionNode {
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
  @NodeChild(value = "receiver", type = ExpressionNode.class)
  @NodeChild(value = "target", type = ExpressionNode.class)
  @NodeChild(value = "somArr", type = ExpressionNode.class)
  @NodeChild(value = "argArr", type = ToArgumentsArrayNode.class,
      executeWith = {"somArr", "target"})
  @Primitive(selector = "invokeOn:with:", noWrapper = true,
      extraChild = ToArgumentsArrayNodeFactory.class)
  public abstract static class InvokeOnPrim extends EagerlySpecializableNode {
    @Child private InvokeOnCache callNode = InvokeOnCache.create();

    public abstract Object executeEvaluated(VirtualFrame frame, SInvokable receiver,
        Object target, SArray somArr);

    @Override
    public final Object doPreEvaluated(final VirtualFrame frame,
        final Object[] args) {
      return executeEvaluated(frame, (SInvokable) args[0], args[1], (SArray) args[2]);
    }

    @Override
    public final Object doPreUnary(final VirtualFrame frame, final Object rcvr) {
      CompilerDirectives.transferToInterpreter();
      throw new UnsupportedOperationException();
    }

    @Override
    public final Object doPreBinary(final VirtualFrame frame, final Object rcvr,
        final Object arg) {
      CompilerDirectives.transferToInterpreter();
      throw new UnsupportedOperationException();
    }

    @Override
    public final Object doPreTernary(final VirtualFrame frame, final Object rcvr,
        final Object arg1, final Object arg2) {
      return executeEvaluated(frame, (SInvokable) rcvr, arg1, (SArray) arg2);
    }

    @Override
    public final Object doPreQuat(final VirtualFrame frame, final Object rcvr,
        final Object arg1,
        final Object arg2, final Object arg3) {
      CompilerDirectives.transferToInterpreter();
      throw new UnsupportedOperationException();
    }

    @Specialization
    public final Object doInvoke(final VirtualFrame frame,
        final SInvokable receiver, final Object target, final SArray somArr,
        final Object[] argArr) {
      return callNode.executeDispatch(frame, receiver, argArr);
    }

    @Override
    public ExpressionNode wrapInEagerWrapper(final SSymbol selector,
        final ExpressionNode[] arguments, final Universe context) {
      throw new NotYetImplementedException();
    }
  }
}
