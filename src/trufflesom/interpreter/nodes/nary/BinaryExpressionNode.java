package trufflesom.interpreter.nodes.nary;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.frame.VirtualFrame;

import bd.primitives.nodes.WithContext;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.vm.Universe;


@NodeChild(value = "receiver", type = ExpressionNode.class)
@NodeChild(value = "argument", type = ExpressionNode.class)
public abstract class BinaryExpressionNode extends ExpressionNode {

  public abstract ExpressionNode getReceiver();

  public abstract ExpressionNode getArgument();

  public abstract Object executeEvaluated(VirtualFrame frame, Object receiver,
      Object argument);

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
