package som.interpreter.nodes;

import som.interpreter.SArguments;
import som.vmobjects.SClass;

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

    private final SClass superClass;

    public NonLocalSuperReadNode(final int contextLevel,
        final SClass superClass, final SourceSection source) {
      super(SArguments.RCVR_IDX, contextLevel, source);
      this.superClass = superClass;
    }

    public final SClass getSuperClass() {
      return superClass;
    }
  }
}
