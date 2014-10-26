package som.interpreter.nodes.literals;

import som.interpreter.Inliner;
import som.interpreter.Invokable;
import som.vm.Universe;
import som.vmobjects.SBlock;
import som.vmobjects.SInvokable;
import som.vmobjects.SInvokable.SMethod;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

public abstract class BlockNode   {
  public static final class BlockNodeWithContext extends LiteralNode {
    protected final SMethod  blockMethod;

    public BlockNodeWithContext(final SMethod blockMethod,
        final SourceSection source) {
      super(source);
      this.blockMethod  = blockMethod;
    }

    public BlockNodeWithContext(final BlockNodeWithContext node) {
      this(node.blockMethod, node.getSourceSection());
    }

    @Override
    public SBlock executeSBlock(final VirtualFrame frame) {
      return Universe.newBlock(blockMethod, frame.materialize());
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      return executeSBlock(frame);
    }

    protected SInvokable cloneMethod(final Inliner inliner) {
      Invokable clonedInvokable = blockMethod.getInvokable().
          cloneWithNewLexicalContext(inliner.getLexicalContext());
      SInvokable forInlining = Universe.newMethod(blockMethod.getSignature(),
          clonedInvokable, false, new SMethod[0]);
      return forInlining;
    }

    @Override
    public void replaceWithIndependentCopyForInlining(final Inliner inliner) {
      SMethod forInlining = (SMethod) cloneMethod(inliner);
      replace(new BlockNodeWithContext(forInlining, getSourceSection()));
    }
  }
}
