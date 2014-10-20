package som.interpreter.nodes;

import som.interpreter.SArguments;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

public abstract class ArgumentReadNode {

  public static class NonLocalArgumentReadNode extends ContextualNode {
    protected final int argumentIndex;

    public NonLocalArgumentReadNode(final int argumentIndex,
        final int contextLevel, final SourceSection source) {
      super(contextLevel, source);
      this.argumentIndex = argumentIndex;
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      return SArguments.arg(determineContext(frame), argumentIndex);
    }
  }

  public static final class NonLocalSuperReadNode extends
      NonLocalArgumentReadNode {

    private final SSymbol holderClass;
    private final boolean classSide;

    public NonLocalSuperReadNode(final int contextLevel,
        final SSymbol holderClass, final boolean classSide,
        final SourceSection source) {
      super(SArguments.RCVR_IDX, contextLevel, source);
      this.holderClass = holderClass;
      this.classSide   = classSide;
    }

    public boolean isClassSide() {
      return classSide;
    }

    public SSymbol getHolderClass() {
      return holderClass;
    }
  }
}
