package som.interpreter.nodes;

import static som.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;
import som.compiler.Variable.Local;
import som.interpreter.nodes.NonLocalVariableNode.NonLocalVariableReadNode;
import som.interpreter.nodes.NonLocalVariableNode.NonLocalVariableWriteNode;
import som.interpreter.nodes.NonLocalVariableNodeFactory.NonLocalVariableReadNodeFactory;
import som.interpreter.nodes.NonLocalVariableNodeFactory.NonLocalVariableWriteNodeFactory;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;


public abstract class UninitializedVariableNode extends ContextualNode {
  protected final Local variable;

  public UninitializedVariableNode(final Local variable,
      final int contextLevel, final SourceSection source) {
    super(contextLevel, source);
    this.variable = variable;
  }

  public static final class UninitializedVariableReadNode extends UninitializedVariableNode {
    public UninitializedVariableReadNode(final Local variable,
        final int contextLevel, final SourceSection source) {
      super(variable, contextLevel, source);
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      transferToInterpreterAndInvalidate("UninitializedVariableReadNode");

      NonLocalVariableReadNode node = NonLocalVariableReadNodeFactory.create(
            contextLevel, variable.getSlot(), getSourceSection());
      return replace(node).executeGeneric(frame);
    }
  }

  public static final class UninitializedVariableWriteNode extends UninitializedVariableNode {
    @Child private ExpressionNode exp;

    public UninitializedVariableWriteNode(final Local variable,
        final int contextLevel, final ExpressionNode exp,
        final SourceSection source) {
      super(variable, contextLevel, source);
      this.exp = exp;
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      transferToInterpreterAndInvalidate("UninitializedVariableWriteNode");

      NonLocalVariableWriteNode node = NonLocalVariableWriteNodeFactory.create(
            contextLevel, variable.getSlot(), getSourceSection(), exp);
      return replace(node).executeGeneric(frame);
    }
  }
}
