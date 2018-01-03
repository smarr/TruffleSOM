package som.interpreter.nodes.nary;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.frame.VirtualFrame;

import bd.nodes.WithContext;
import som.interpreter.nodes.ExpressionNode;
import som.vm.Universe;
import som.vmobjects.SSymbol;


@NodeChildren({
    @NodeChild(value = "receiver", type = ExpressionNode.class),
    @NodeChild(value = "argument", type = ExpressionNode.class)})
public abstract class BinaryExpressionNode extends EagerlySpecializableNode {

  public abstract Object executeEvaluated(VirtualFrame frame, Object receiver,
      Object argument);

  @Override
  public final Object doPreEvaluated(final VirtualFrame frame,
      final Object[] arguments) {
    return executeEvaluated(frame, arguments[0], arguments[1]);
  }

  @Override
  public EagerPrimitive wrapInEagerWrapper(final SSymbol selector,
      final ExpressionNode[] arguments, final Universe universe) {
    return new EagerBinaryPrimitiveNode(selector, arguments[0], arguments[1], this,
        universe).initialize(sourceSection);
  }

  @GenerateNodeFactory
  public abstract static class BinarySystemOperation extends BinaryExpressionNode
      implements WithContext<BinarySystemOperation, Universe> {
    @CompilationFinal protected Universe universe;

    @Override
    public BinarySystemOperation initialize(final Universe universe) {
      assert this.universe == null && universe != null;
      this.universe = universe;
      return this;
    }
  }
}
