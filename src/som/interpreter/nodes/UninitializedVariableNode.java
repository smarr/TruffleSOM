package som.interpreter.nodes;

import static som.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;
import som.compiler.Variable;
import som.compiler.Variable.Argument;
import som.compiler.Variable.Local;
import som.interpreter.Inliner;
import som.interpreter.nodes.NonLocalVariableNode.NonLocalSuperReadNode;
import som.interpreter.nodes.NonLocalVariableNode.NonLocalVariableReadNode;
import som.interpreter.nodes.NonLocalVariableNode.NonLocalVariableWriteNode;
import som.interpreter.nodes.NonLocalVariableNodeFactory.NonLocalVariableReadNodeFactory;
import som.interpreter.nodes.NonLocalVariableNodeFactory.NonLocalVariableWriteNodeFactory;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;


public abstract class UninitializedVariableNode extends ContextualNode {
  protected final Variable variable;

  public UninitializedVariableNode(final Variable variable,
      final int contextLevel, final FrameSlot localSelf, final SourceSection source) {
    super(contextLevel, localSelf, source);
    this.variable = variable;
  }

  public static final class UninitializedVariableReadNode extends UninitializedVariableNode {
    public UninitializedVariableReadNode(final Variable variable,
        final int contextLevel, final FrameSlot localSelf, final SourceSection source) {
      super(variable, contextLevel, localSelf, source);
    }

    public UninitializedVariableReadNode(final UninitializedVariableReadNode node,
        final FrameSlot inlinedVarSlot, final FrameSlot inlinedLocalSelfSlot) {
      this(node.variable.cloneForInlining(inlinedVarSlot), node.contextLevel,
          inlinedLocalSelfSlot, node.getSourceSection());
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      transferToInterpreterAndInvalidate("UninitializedVariableReadNode");

      assert frame.getFrameDescriptor().findFrameSlot(localSelf.getIdentifier()) == localSelf;

      NonLocalVariableReadNode node = NonLocalVariableReadNodeFactory.create(
            contextLevel, variable.slot, localSelf, getSourceSection());
      return replace(node).executeGeneric(frame);
    }

    @Override
    public void replaceWithIndependentCopyForInlining(final Inliner inliner) {
      FrameSlot localSelfSlot = inliner.getLocalFrameSlot(getLocalSelfSlotIdentifier());
      FrameSlot varSlot       = inliner.getFrameSlot(this, variable.getSlotIdentifier());
      assert localSelfSlot != null;
      assert varSlot       != null;
      replace(new UninitializedVariableReadNode(this, varSlot, localSelfSlot));
    }

    public boolean accessesArgument() {
      return variable instanceof Argument;
    }

    public boolean accessesTemporary() {
      return variable instanceof Local;
    }

    public boolean accessesSelf() {
      if (accessesTemporary()) {
        return false;
      }
      return ((Argument) variable).isSelf();
    }

    public int getArgumentIndex() {
      CompilerAsserts.neverPartOfCompilation("getArgumentIndex");
      if (!accessesArgument()) {
        throw new UnsupportedOperationException("This node does not access an argument.");
      }

      return ((Argument) variable).index;
    }
  }

  public static final class UninitializedVariableWriteNode extends UninitializedVariableNode {
    @Child private ExpressionNode exp;

    public UninitializedVariableWriteNode(final Local variable,
        final int contextLevel, final FrameSlot localSelf,
        final ExpressionNode exp, final SourceSection source) {
      super(variable, contextLevel, localSelf, source);
      this.exp = exp;
    }

    public UninitializedVariableWriteNode(final UninitializedVariableWriteNode node,
        final FrameSlot inlinedVarSlot, final FrameSlot inlinedLocalSelfSlot) {
      this((Local) node.variable.cloneForInlining(inlinedVarSlot),
          node.contextLevel, inlinedLocalSelfSlot, node.exp,
          node.getSourceSection());
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      transferToInterpreterAndInvalidate("UninitializedVariableWriteNode");

      assert frame.getFrameDescriptor().findFrameSlot(localSelf.getIdentifier()) == localSelf;
      NonLocalVariableWriteNode node = NonLocalVariableWriteNodeFactory.create(
            contextLevel, variable.slot, localSelf, getSourceSection(), exp);
      return replace(node).executeGeneric(frame);
    }

    @Override
    public void replaceWithIndependentCopyForInlining(final Inliner inliner) {
      FrameSlot localSelfSlot = inliner.getLocalFrameSlot(getLocalSelfSlotIdentifier());
      FrameSlot varSlot       = inliner.getFrameSlot(this, variable.getSlotIdentifier());
      assert localSelfSlot != null;
      assert varSlot       != null;
      replace(new UninitializedVariableWriteNode(this, varSlot, localSelfSlot));
    }
  }
}
