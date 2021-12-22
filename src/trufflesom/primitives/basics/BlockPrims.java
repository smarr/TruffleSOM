package trufflesom.primitives.basics;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.ReportPolymorphism.Megamorphic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.nodes.DirectCallNode;

import bd.primitives.Primitive;
import bd.primitives.nodes.PreevaluatedExpression;
import trufflesom.interpreter.bc.RestartLoopException;
import trufflesom.interpreter.nodes.nary.BinaryExpressionNode;
import trufflesom.interpreter.nodes.nary.QuaternaryExpressionNode;
import trufflesom.interpreter.nodes.nary.TernaryExpressionNode;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.VmSettings;
import trufflesom.vmobjects.SAbstractObject;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SInvokable;


public abstract class BlockPrims {
  protected static int InlineCacheSize = 6;

  public static final DirectCallNode createCallNode(final SInvokable method) {
    return Truffle.getRuntime().createDirectCallNode(method.getCallTarget());
  }

  @GenerateNodeFactory
  @Primitive(className = "Block", primitive = "restart")
  public abstract static class RestartPrim extends UnaryExpressionNode {
    @Specialization
    public SAbstractObject doSBlock(final SBlock receiver) {
      assert VmSettings.UseBcInterp : "This primitive is not supported in the AST interpreter "
          + "Perhaps something went wrong with the intrinsification of "
          + "#whileTrue:/#whileFalse:?";
      throw new RestartLoopException();
    }
  }

  @ReportPolymorphism
  @GenerateNodeFactory
  @GenerateWrapper
  @Primitive(className = "Block", primitive = "value")
  @Primitive(className = "Block1", primitive = "value")
  @Primitive(selector = "value", inParser = false,
      receiverType = {SBlock.class, Boolean.class})
  @ImportStatic(BlockPrims.class)
  public abstract static class ValueNonePrim extends UnaryExpressionNode {

    public abstract Object executeEvaluated(VirtualFrame frame, SBlock receiver);

    @Specialization(
        guards = {"receiver.getMethod() == method", "!method.isTrivial()"},
        limit = "InlineCacheSize")
    public final Object doSBlock(final SBlock receiver,
        @Cached("receiver.getMethod()") final SInvokable method,
        @Cached("createCallNode(method)") final DirectCallNode call) {
      return call.call(receiver);
    }

    @Specialization(
        guards = {"receiver.getMethod() == method", "method.isTrivial()"},
        limit = "InlineCacheSize")
    public final Object doTrivial(final SBlock receiver,
        @Cached("receiver.getMethod()") final SInvokable method,
        @Cached("method.copyTrivialNode()") final PreevaluatedExpression expr) {
      return expr.doPreEvaluated(null, new Object[] {receiver});
    }

    @Specialization
    @Megamorphic
    public final Object generic(final SBlock receiver) {
      return receiver.getMethod().invoke(new Object[] {receiver});
    }

    @Specialization
    public final boolean doBoolean(final boolean receiver) {
      return receiver;
    }

    @Override
    public WrapperNode createWrapper(final ProbeNode probe) {
      return new ValueNonePrimWrapper(this, probe);
    }
  }

  @ReportPolymorphism
  @GenerateWrapper
  @GenerateNodeFactory
  @Primitive(className = "Block2", primitive = "value:", selector = "value:", inParser = false,
      receiverType = SBlock.class)
  @ImportStatic(BlockPrims.class)
  public abstract static class ValueOnePrim extends BinaryExpressionNode {

    public abstract Object executeEvaluated(VirtualFrame frame, SBlock receiver, Object arg);

    @Specialization(
        guards = {"receiver.getMethod() == method", "!method.isTrivial()"},
        limit = "InlineCacheSize")
    public final Object doSBlock(final SBlock receiver, final Object arg,
        @Cached("receiver.getMethod()") final SInvokable method,
        @Cached("createCallNode(method)") final DirectCallNode call) {
      return call.call(receiver, arg);
    }

    @Specialization(
        guards = {"receiver.getMethod() == method", "method.isTrivial()"},
        limit = "InlineCacheSize")
    public final Object doTrivial(final SBlock receiver, final Object arg,
        @Cached("receiver.getMethod()") final SInvokable method,
        @Cached("method.copyTrivialNode()") final PreevaluatedExpression expr) {
      return expr.doPreEvaluated(null, new Object[] {receiver, arg});
    }

    @Specialization
    @Megamorphic
    public final Object generic(final SBlock receiver, final Object arg) {
      return receiver.getMethod().invoke(new Object[] {receiver, arg});
    }

    @Fallback
    public final Object makeGenericSend(final Object receiver, final Object argument) {
      return makeGenericSend(SymbolTable.symbolFor("value:")).doPreEvaluated(null,
          new Object[] {receiver, argument});
    }

    @Override
    public WrapperNode createWrapper(final ProbeNode probe) {
      return new ValueOnePrimWrapper(this, probe);
    }
  }

  @ReportPolymorphism
  @GenerateNodeFactory
  @Primitive(className = "Block3", primitive = "value:with:", selector = "value:with:",
      inParser = false, receiverType = SBlock.class)
  @ImportStatic(BlockPrims.class)
  public abstract static class ValueTwoPrim extends TernaryExpressionNode {

    public abstract Object executeEvaluated(SBlock receiver, Object arg1, Object arg2);

    @Specialization(
        guards = {"receiver.getMethod() == method", "!method.isTrivial()"},
        limit = "InlineCacheSize")
    public final Object doSBlock(final SBlock receiver, final Object arg1, final Object arg2,
        @Cached("receiver.getMethod()") final SInvokable method,
        @Cached("createCallNode(method)") final DirectCallNode call) {
      return call.call(receiver, arg1, arg2);
    }

    @Specialization(
        guards = {"receiver.getMethod() == method", "method.isTrivial()"},
        limit = "InlineCacheSize")
    public final Object doTrivial(final SBlock receiver, final Object arg1, final Object arg2,
        @Cached("receiver.getMethod()") final SInvokable method,
        @Cached("method.copyTrivialNode()") final PreevaluatedExpression expr) {
      return expr.doPreEvaluated(null, new Object[] {receiver, arg1, arg2});
    }

    @Specialization
    @Megamorphic
    public final Object generic(final SBlock receiver, final Object arg1, final Object arg2) {
      return receiver.getMethod().invoke(new Object[] {receiver, arg1, arg2});
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Block4", primitive = "value:with:with:")
  public abstract static class ValueMorePrim extends QuaternaryExpressionNode {
    @Specialization
    public final Object doSBlock(final VirtualFrame frame,
        final SBlock receiver, final Object firstArg, final Object secondArg,
        final Object thirdArg) {
      CompilerDirectives.transferToInterpreter();
      throw new RuntimeException(
          "This should never be called, because SOM Blocks have max. 2 arguments.");
    }
  }
}
