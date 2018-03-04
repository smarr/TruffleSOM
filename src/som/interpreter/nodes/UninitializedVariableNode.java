package som.interpreter.nodes;

import static som.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;

import com.oracle.truffle.api.frame.VirtualFrame;

import bd.inlining.ScopeAdaptationVisitor;
import som.compiler.Variable.Local;
import som.interpreter.nodes.LocalVariableNode.LocalVariableReadNode;
import som.interpreter.nodes.LocalVariableNode.LocalVariableWriteNode;
import som.interpreter.nodes.LocalVariableNodeFactory.LocalVariableReadNodeGen;
import som.interpreter.nodes.LocalVariableNodeFactory.LocalVariableWriteNodeGen;
import som.interpreter.nodes.NonLocalVariableNode.NonLocalVariableReadNode;
import som.interpreter.nodes.NonLocalVariableNode.NonLocalVariableWriteNode;
import som.interpreter.nodes.NonLocalVariableNodeFactory.NonLocalVariableReadNodeGen;
import som.interpreter.nodes.NonLocalVariableNodeFactory.NonLocalVariableWriteNodeGen;


public abstract class UninitializedVariableNode extends ContextualNode {
  protected final Local variable;

  public UninitializedVariableNode(final Local variable, final int contextLevel) {
    super(contextLevel);
    this.variable = variable;
  }

  public static final class UninitializedVariableReadNode extends UninitializedVariableNode {
    public UninitializedVariableReadNode(final Local variable, final int contextLevel) {
      super(variable, contextLevel);
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      transferToInterpreterAndInvalidate("UninitializedVariableReadNode");

      if (contextLevel > 0) {
        NonLocalVariableReadNode node = NonLocalVariableReadNodeGen.create(
            contextLevel, variable).initialize(sourceSection);
        return replace(node).executeGeneric(frame);
      } else {
        // assert frame.getFrameDescriptor().findFrameSlot(variable.getSlotIdentifier()) ==
        // variable.getSlot();
        LocalVariableReadNode node =
            LocalVariableReadNodeGen.create(variable).initialize(sourceSection);
        return replace(node).executeGeneric(frame);
      }
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      inliner.updateRead(variable, this, contextLevel);
    }
  }

  public static final class UninitializedVariableWriteNode extends UninitializedVariableNode {
    @Child private ExpressionNode exp;

    public UninitializedVariableWriteNode(final Local variable, final int contextLevel,
        final ExpressionNode exp) {
      super(variable, contextLevel);
      this.exp = exp;
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      transferToInterpreterAndInvalidate("UninitializedVariableWriteNode");

      if (accessesOuterContext()) {
        NonLocalVariableWriteNode node = NonLocalVariableWriteNodeGen.create(
            contextLevel, variable, exp).initialize(sourceSection);
        return replace(node).executeGeneric(frame);
      } else {
        // not sure about removing this assertion :(((
        // assert frame.getFrameDescriptor().findFrameSlot(variable.getSlotIdentifier()) ==
        // variable.getSlot();
        LocalVariableWriteNode node =
            LocalVariableWriteNodeGen.create(variable, exp).initialize(sourceSection);
        return replace(node).executeGeneric(frame);
      }
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      inliner.updateWrite(variable, this, exp, contextLevel);
    }

    @Override
    public String toString() {
      return "UninitVarWrite(" + variable.toString() + ")";
    }
  }
}
