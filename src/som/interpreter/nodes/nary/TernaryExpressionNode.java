package som.interpreter.nodes.nary;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.frame.VirtualFrame;

import bd.primitives.nodes.WithContext;
import som.interpreter.nodes.ExpressionNode;
import som.vm.Universe;
import som.vmobjects.SSymbol;


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
