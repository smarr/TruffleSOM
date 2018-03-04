package som.interpreter.nodes;

import static som.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;

import com.oracle.truffle.api.frame.VirtualFrame;

import som.compiler.Variable.Local;
import som.interpreter.InlinerAdaptToEmbeddedOuterContext;
import som.interpreter.InlinerForLexicallyEmbeddedMethods;
import som.interpreter.SplitterForLexicallyEmbeddedCode;
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
    public void replaceWithIndependentCopyForInlining(
        final SplitterForLexicallyEmbeddedCode inliner) {
      FrameSlot varSlot = inliner.getFrameSlot(this, variable.getSlotIdentifier());
      assert varSlot != null;
      replace(new UninitializedVariableReadNode(this, varSlot).initialize(sourceSection));
    }

    @Override
    public void replaceWithLexicallyEmbeddedNode(
        final InlinerForLexicallyEmbeddedMethods inliner) {
      UninitializedVariableReadNode inlined;

      if (contextLevel == 0) {
        // might need to add new frame slot in outer method
        inlined = inliner.getLocalRead(variable.getSlotIdentifier(),
            getSourceSection());
      } else {
        inlined = new UninitializedVariableReadNode(
            variable, contextLevel - 1).initialize(sourceSection);
      }
      replace(inlined);
    }

    @Override
    public void replaceWithCopyAdaptedToEmbeddedOuterContext(
        final InlinerAdaptToEmbeddedOuterContext inliner) {
      // if the context level is 1, the variable is in the outer context,
      // which just got inlined, so, we need to adapt the slot id
      UninitializedVariableReadNode node;
      if (inliner.appliesTo(contextLevel)) {
        node = new UninitializedVariableReadNode(this,
            inliner.getOuterSlot(variable.getSlotIdentifier())).initialize(sourceSection);
        replace(node);
        return;
      } else if (inliner.needToAdjustLevel(contextLevel)) {
        node = new UninitializedVariableReadNode(
            variable, contextLevel - 1).initialize(sourceSection);
        replace(node);
        return;
      }
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
    public void replaceWithIndependentCopyForInlining(
        final SplitterForLexicallyEmbeddedCode inliner) {
      FrameSlot varSlot = inliner.getFrameSlot(this, variable.getSlotIdentifier());
      assert varSlot != null;
      replace(new UninitializedVariableWriteNode(this, varSlot).initialize(sourceSection));
    }

    @Override
    public void replaceWithCopyAdaptedToEmbeddedOuterContext(
        final InlinerAdaptToEmbeddedOuterContext inliner) {
      // if the context level is 1, the variable is in the outer context,
      // which just got inlined, so, we need to adapt the slot id
      UninitializedVariableWriteNode node;
      if (inliner.appliesTo(contextLevel)) {
        node = new UninitializedVariableWriteNode(this,
            inliner.getOuterSlot(variable.getSlotIdentifier())).initialize(sourceSection);
        replace(node);
        return;
      } else if (inliner.needToAdjustLevel(contextLevel)) {
        node = new UninitializedVariableWriteNode(
            variable, contextLevel - 1, exp).initialize(sourceSection);
        replace(node);
        return;
      }
    }

    @Override
    public String toString() {
      return "UninitVarWrite(" + variable.toString() + ")";
    }

    @Override
    public void replaceWithLexicallyEmbeddedNode(
        final InlinerForLexicallyEmbeddedMethods inliner) {
      UninitializedVariableWriteNode inlined;

      if (contextLevel == 0) {
        // might need to add new frame slot in outer method
        inlined = inliner.getLocalWrite(variable.getSlotIdentifier(),
            exp, getSourceSection());
      } else {
        inlined = new UninitializedVariableWriteNode(
            variable, contextLevel - 1, exp).initialize(sourceSection);
      }
      replace(inlined);
    }
  }
}
