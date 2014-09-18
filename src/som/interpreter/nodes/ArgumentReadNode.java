package som.interpreter.nodes;

import som.interpreter.SArguments;
import som.vmobjects.SClass;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

public abstract class ArgumentReadNode {

  public static class LocalArgumentReadNode extends ExpressionNode
      implements PreevaluatedExpression {
    protected final int argumentIndex;

    public LocalArgumentReadNode(final int argumentIndex, final SourceSection source) {
      super(source);
      assert argumentIndex >= 0;
      this.argumentIndex = argumentIndex;
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      return SArguments.arg(frame, argumentIndex);
    }

    @Override
    public final Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return arguments[argumentIndex];
    }
  }

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
      NonLocalArgumentReadNode implements ISuperReadNode {

    private final SClass superClass;

    public NonLocalSuperReadNode(final int contextLevel,
        final SClass superClass, final SourceSection source) {
      super(SArguments.RCVR_IDX, contextLevel, source);
      this.superClass = superClass;
    }

    @Override
    public final SClass getSuperClass() {
      return superClass;
    }
  }
}
