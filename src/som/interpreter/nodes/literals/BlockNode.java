package som.interpreter.nodes.literals;

import som.vm.Universe;
import som.vmobjects.SBlock;
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

    @Override
    public SBlock executeSBlock(final VirtualFrame frame) {
      return Universe.newBlock(blockMethod, frame.materialize());
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      return executeSBlock(frame);
    }
  }
}
