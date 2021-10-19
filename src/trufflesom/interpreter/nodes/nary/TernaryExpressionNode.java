package trufflesom.interpreter.nodes.nary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.frame.VirtualFrame;

import bd.primitives.nodes.WithContext;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SSymbol;


@NodeChild(value = "receiver", type = ExpressionNode.class)
@NodeChild(value = "firstArg", type = ExpressionNode.class)
@NodeChild(value = "secondArg", type = ExpressionNode.class)
public abstract class TernaryExpressionNode extends EagerlySpecializableNode {

  public abstract Object executeEvaluated(VirtualFrame frame, Object receiver, Object firstArg,
      Object secondArg);

  @Override
  public final Object doPreEvaluated(final VirtualFrame frame,
      final Object[] arguments) {
    return executeEvaluated(frame, arguments[0], arguments[1], arguments[2]);
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
    return executeEvaluated(frame, rcvr, arg1, arg2);
  }

  @Override
  public final Object doPreQuat(final VirtualFrame frame, final Object rcvr, final Object arg1,
      final Object arg2, final Object arg3) {
    CompilerDirectives.transferToInterpreter();
    throw new UnsupportedOperationException();
  }

  @Override
  public EagerPrimitive wrapInEagerWrapper(final SSymbol selector,
      final ExpressionNode[] arguments, final Universe universe) {
    return new EagerTernaryPrimitiveNode(selector, arguments[0], arguments[1], arguments[2],
        this, universe).initialize(sourceSection);
  }

  public abstract static class TernarySystemOperation extends TernaryExpressionNode
      implements WithContext<TernarySystemOperation, Universe> {
    @CompilationFinal protected Universe universe;

    @Override
    public TernarySystemOperation initialize(final Universe universe) {
      assert this.universe == null && universe != null;
      this.universe = universe;
      return this;
    }
  }
}
