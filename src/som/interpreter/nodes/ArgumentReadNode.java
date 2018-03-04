package som.interpreter.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import som.interpreter.InlinerAdaptToEmbeddedOuterContext;
import som.interpreter.InlinerForLexicallyEmbeddedMethods;
import som.interpreter.SArguments;
import som.vmobjects.SSymbol;


public abstract class ArgumentReadNode {

  public static class LocalArgumentReadNode extends ExpressionNode {
    protected final int      argumentIndex;
    protected final Argument arg;

    public LocalArgumentReadNode(final Argument arg) {
      assert arg.index >= 0;
      this.arg = arg;
      this.argumentIndex = arg.index;
    }

    /** Only to be used in primitives. */
    public LocalArgumentReadNode(final boolean useInPrim, final int idx) {
      assert idx >= 0;
      this.arg = null;
      this.argumentIndex = idx;
      assert useInPrim;
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      return SArguments.arg(frame, argumentIndex);
    }

    @Override
    public void replaceWithLexicallyEmbeddedNode(
        final InlinerForLexicallyEmbeddedMethods inliner) {
      replace(inliner.getReplacementForLocalArgument(argumentIndex,
          getSourceSection()));
    }
  }

  public static class NonLocalArgumentReadNode extends ContextualNode {
    protected final int      argumentIndex;
    protected final Argument arg;

    public NonLocalArgumentReadNode(final Argument arg, final int contextLevel) {
      super(contextLevel);
      assert contextLevel > 0;
      this.arg = arg;
      this.argumentIndex = arg.index;
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      return SArguments.arg(determineContext(frame), argumentIndex);
    }

    @Override
    public void replaceWithLexicallyEmbeddedNode(
        final InlinerForLexicallyEmbeddedMethods inliner) {
      ExpressionNode inlined;
      if (contextLevel == 1) {
        inlined = createLocalNode();
      } else {
        inlined = createNonLocalNode();
      }
      replace(inlined);
    }

    protected NonLocalArgumentReadNode createNonLocalNode() {
      return new NonLocalArgumentReadNode(
          argumentIndex, contextLevel - 1).initialize(sourceSection);
    }

    protected LocalArgumentReadNode createLocalNode() {
      return new LocalArgumentReadNode(argumentIndex).initialize(sourceSection);
    }

    @Override
    public void replaceWithCopyAdaptedToEmbeddedOuterContext(
        final InlinerAdaptToEmbeddedOuterContext inliner) {
      // this should be the access to a block argument
      if (inliner.appliesTo(contextLevel)) {
        assert !(this instanceof NonLocalSuperReadNode);
        ExpressionNode node =
            inliner.getReplacementForBlockArgument(argumentIndex, getSourceSection());
        replace(node);
        return;
      } else if (inliner.needToAdjustLevel(contextLevel)) {
        // in the other cases, we just need to adjust the context level
        NonLocalArgumentReadNode node = createNonLocalNode();
        replace(node);
        return;
      }
    }
  }

  public static final class LocalSuperReadNode extends LocalArgumentReadNode
      implements ISuperReadNode {

    private final SSymbol holderClass;
    private final boolean classSide;

    public LocalSuperReadNode(final Argument arg, final SSymbol holderClass,
        final boolean classSide) {
      super(arg);
      this.holderClass = holderClass;
      this.classSide = classSide;
    }

    @Override
    public SSymbol getHolderClass() {
      return holderClass;
    }

    @Override
    public boolean isClassSide() {
      return classSide;
    }
  }

  public static final class NonLocalSuperReadNode extends
      NonLocalArgumentReadNode implements ISuperReadNode {

    private final SSymbol holderClass;
    private final boolean classSide;

    public NonLocalSuperReadNode(final Argument arg, final int contextLevel,
        final SSymbol holderClass, final boolean classSide) {
      super(arg, contextLevel);
      this.holderClass = holderClass;
      this.classSide = classSide;
    }

    @Override
    public SSymbol getHolderClass() {
      return holderClass;
    }

    @Override
    protected NonLocalArgumentReadNode createNonLocalNode() {
      return new NonLocalSuperReadNode(
          contextLevel - 1, holderClass, classSide).initialize(sourceSection);
    }

    @Override
    protected LocalArgumentReadNode createLocalNode() {
      return new LocalSuperReadNode(holderClass, classSide).initialize(sourceSection);
    }

    @Override
    public boolean isClassSide() {
      return classSide;
    }
  }
}
