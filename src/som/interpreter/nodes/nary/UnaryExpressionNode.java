package som.interpreter.nodes.nary;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.frame.VirtualFrame;

import bd.primitives.nodes.WithContext;
import som.interpreter.nodes.ExpressionNode;
import som.vm.Universe;
import som.vmobjects.SSymbol;


@NodeChild(value = "receiver", type = ExpressionNode.class)
public abstract class UnaryExpressionNode extends EagerlySpecializableNode {

  public abstract Object executeEvaluated(VirtualFrame frame, Object receiver);

  @Override
  public final Object doPreEvaluated(final VirtualFrame frame,
      final Object[] arguments) {
    return executeEvaluated(frame, arguments[0]);
  }

  @Override
  public EagerPrimitive wrapInEagerWrapper(final SSymbol selector,
      final ExpressionNode[] arguments, final Universe universe) {
    return new EagerUnaryPrimitiveNode(
        selector, arguments[0], this, universe).initialize(sourceSection);
  }

  public abstract static class UnarySystemOperation extends UnaryExpressionNode
      implements WithContext<UnarySystemOperation, Universe> {
    @CompilationFinal protected Universe universe;

    @Override
    public UnarySystemOperation initialize(final Universe universe) {
      assert this.universe == null && universe != null;
      this.universe = universe;
      return this;
    }
  }
}
