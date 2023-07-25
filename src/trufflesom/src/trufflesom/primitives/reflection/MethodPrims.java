package trufflesom.primitives.reflection;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;

import bdt.primitives.Primitive;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.nary.EagerlySpecializableNode;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.primitives.arrays.ToArgumentsArrayNode;
import trufflesom.primitives.arrays.ToArgumentsArrayNodeFactory;
import trufflesom.vmobjects.SAbstractObject;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SInvokable;


public final class MethodPrims {

  @GenerateNodeFactory
  @Primitive(className = "Method", primitive = "signature")
  @Primitive(className = "Primitive", primitive = "signature")
  public abstract static class SignaturePrim extends UnaryExpressionNode {
    @Specialization
    public static final SAbstractObject doSMethod(final SInvokable receiver) {
      return receiver.getSignature();
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Method", primitive = "holder")
  @Primitive(className = "Primitive", primitive = "holder")
  public abstract static class HolderPrim extends UnaryExpressionNode {
    @Specialization
    public static final SAbstractObject doSMethod(final SInvokable receiver) {
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
  @Primitive(selector = "invokeOn:with:", extraChild = ToArgumentsArrayNodeFactory.class)
  public abstract static class InvokeOnPrim extends EagerlySpecializableNode {

    public abstract Object executeEvaluated(VirtualFrame frame, SInvokable receiver,
        Object target, SArray somArr);

    @Override
    public final Object doPreEvaluated(final VirtualFrame frame,
        final Object[] args) {
      return executeEvaluated(frame, (SInvokable) args[0], args[1], (SArray) args[2]);
    }

    @Specialization(guards = "receiver == cachedReceiver",
        limit = "" + AbstractDispatchNode.INLINE_CACHE_SIZE)
    public static final Object doCached(
        final SInvokable receiver, final Object target, final SArray somArr,
        final Object[] argArr,
        @Cached("receiver") final SInvokable cachedReceiver,
        @Cached("create(receiver.getCallTarget())") final DirectCallNode callNode) {
      return callNode.call(argArr);
    }

    @Specialization(replaces = "doCached")
    public static final Object doUncached(
        final SInvokable receiver, final Object target, final SArray somArr,
        final Object[] argArr,
        @Cached final IndirectCallNode callNode) {
      return callNode.call(receiver.getCallTarget(), argArr);
    }
  }
}
