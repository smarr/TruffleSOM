package trufflesom.interpreter.nodes;

import static trufflesom.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;

import com.oracle.truffle.api.frame.VirtualFrame;

import bd.inlining.ScopeAdaptationVisitor;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.nodes.LocalVariableNode.LocalVariableReadNode;
import trufflesom.interpreter.nodes.LocalVariableNode.LocalVariableWriteNode;
import trufflesom.interpreter.nodes.LocalVariableNodeFactory.LocalVariableReadNodeGen;
import trufflesom.interpreter.nodes.LocalVariableNodeFactory.LocalVariableWriteNodeGen;
import trufflesom.interpreter.nodes.NonLocalVariableNode.NonLocalVariableReadNode;
import trufflesom.interpreter.nodes.NonLocalVariableNode.NonLocalVariableWriteNode;
import trufflesom.interpreter.nodes.NonLocalVariableNodeFactory.NonLocalVariableReadNodeGen;
import trufflesom.interpreter.nodes.NonLocalVariableNodeFactory.NonLocalVariableWriteNodeGen;


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

    public Local getLocal() {
      return variable;
    }
  }
}
