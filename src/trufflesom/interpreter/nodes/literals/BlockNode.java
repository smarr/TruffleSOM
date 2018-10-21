package trufflesom.interpreter.nodes.literals;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.compiler.MethodGenerationContext;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.InlinerAdaptToEmbeddedOuterContext;
import trufflesom.interpreter.InlinerForLexicallyEmbeddedMethods;
import trufflesom.interpreter.Invokable;
import trufflesom.interpreter.Method;
import trufflesom.interpreter.SplitterForLexicallyEmbeddedCode;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable.SMethod;


public class BlockNode extends LiteralNode {

  protected final SMethod            blockMethod;
  @CompilationFinal protected SClass blockClass;

  protected final Universe universe;

  public BlockNode(final SMethod blockMethod, final SourceSection source,
      final Universe universe) {
    super(source);
    this.blockMethod = blockMethod;
    this.universe = universe;
  }

  public SMethod getMethod() {
    return blockMethod;
  }

  protected void setBlockClass() {
    blockClass = universe.getBlockClass(blockMethod.getNumberOfArguments());
  }

  @Override
  public SBlock executeSBlock(final VirtualFrame frame) {
    if (blockClass == null) {
      CompilerDirectives.transferToInterpreter();
      setBlockClass();
    }
    return Universe.newBlock(blockMethod, blockClass, null);
  }

  @Override
  public final Object executeGeneric(final VirtualFrame frame) {
    return executeSBlock(frame);
  }

  @Override
  public void replaceWithIndependentCopyForInlining(
      final SplitterForLexicallyEmbeddedCode inliner) {
    Invokable clonedInvokable =
        blockMethod.getInvokable().cloneWithNewLexicalContext(inliner.getCurrentScope());
    replaceAdapted(clonedInvokable);
  }

  @Override
  public void replaceWithLexicallyEmbeddedNode(
      final InlinerForLexicallyEmbeddedMethods inliner) {
    Invokable adapted =
        ((Method) blockMethod.getInvokable()).cloneAndAdaptToEmbeddedOuterContext(inliner);
    replaceAdapted(adapted);
  }

  @Override
  public void replaceWithCopyAdaptedToEmbeddedOuterContext(
      final InlinerAdaptToEmbeddedOuterContext inliner) {
    Invokable adapted =
        ((Method) blockMethod.getInvokable()).cloneAndAdaptToSomeOuterContextBeingEmbedded(
            inliner);
    replaceAdapted(adapted);
  }

  private void replaceAdapted(final Invokable adaptedForContext) {
    SMethod adapted = (SMethod) Universe.newMethod(
        blockMethod.getSignature(), adaptedForContext, false,
        blockMethod.getEmbeddedBlocks());
    replace(createNode(adapted));
  }

  protected BlockNode createNode(final SMethod adapted) {
    return new BlockNode(adapted, sourceSection, universe);
  }

  @Override
  public ExpressionNode inline(final MethodGenerationContext mgenc,
      final Local... blockArguments) {
    // self doesn't need to be passed
    assert blockMethod.getNumberOfArguments() - 1 == blockArguments.length;
    return blockMethod.getInvokable().inline(mgenc, blockArguments);
  }

  public static final class BlockNodeWithContext extends BlockNode {

    public BlockNodeWithContext(final SMethod blockMethod, final SourceSection source,
        final Universe universe) {
      super(blockMethod, source, universe);
    }

    public BlockNodeWithContext(final BlockNodeWithContext node) {
      this(node.blockMethod, node.sourceSection, node.universe);
    }

    @Override
    public SBlock executeSBlock(final VirtualFrame frame) {
      if (blockClass == null) {
        CompilerDirectives.transferToInterpreter();
        setBlockClass();
      }
      return Universe.newBlock(blockMethod, blockClass, frame.materialize());
    }

    @Override
    protected BlockNode createNode(final SMethod adapted) {
      return new BlockNodeWithContext(adapted, sourceSection, universe);
    }
  }
}
