package trufflesom.interpreter.nodes.literals;

import java.util.ArrayList;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;

import bd.inlining.ScopeAdaptationVisitor;
import bd.primitives.nodes.PreevaluatedExpression;
import trufflesom.compiler.MethodGenerationContext;
import trufflesom.compiler.Variable;
import trufflesom.compiler.Variable.Argument;
import trufflesom.interpreter.Method;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable.SMethod;


public class BlockNode extends LiteralNode {

  protected final SMethod            blockMethod;
  @CompilationFinal protected SClass blockClass;

  protected final Universe universe;

  public BlockNode(final SMethod blockMethod, final Universe universe) {
    this.blockMethod = blockMethod;
    this.universe = universe;
  }

  public SMethod getMethod() {
    return blockMethod;
  }

  public Argument[] getArguments() {
    Method method = (Method) blockMethod.getInvokable();
    Variable[] variables = method.getScope().getVariables();
    ArrayList<Argument> args = new ArrayList<>();
    for (Variable v : variables) {
      if (v instanceof Argument) {
        args.add((Argument) v);
      }
    }
    return args.toArray(new Argument[0]);
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
    return new SBlock(blockMethod, blockClass, null);
  }

  @Override
  public final Object executeGeneric(final VirtualFrame frame) {
    return executeSBlock(frame);
  }

  @Override
  public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
    if (!inliner.outerScopeChanged()) {
      return;
    }

    Method blockIvk = (Method) blockMethod.getInvokable();
    Method adapted = blockIvk.cloneAndAdaptAfterScopeChange(null, inliner.getScope(blockIvk),
        inliner.contextLevel + 1, true, inliner.outerScopeChanged());
    SMethod method = new SMethod(blockMethod.getSignature(), adapted,
        blockMethod.getEmbeddedBlocks(), blockIvk.getSourceSection());
    method.setHolder(blockMethod.getHolder());
    replace(createNode(method));
  }

  protected BlockNode createNode(final SMethod adapted) {
    return new BlockNode(adapted, universe).initialize(sourceSection);
  }

  @Override
  public ExpressionNode inline(final MethodGenerationContext mgenc) {
    return blockMethod.getInvokable().inline(mgenc, blockMethod);
  }

  public static final class BlockNodeWithContext extends BlockNode {

    public BlockNodeWithContext(final SMethod blockMethod, final Universe universe) {
      super(blockMethod, universe);
    }

    @Override
    public SBlock executeSBlock(final VirtualFrame frame) {
      if (blockClass == null) {
        CompilerDirectives.transferToInterpreter();
        setBlockClass();
      }
      return new SBlock(blockMethod, blockClass, frame.materialize());
    }

    @Override
    protected BlockNode createNode(final SMethod adapted) {
      return new BlockNodeWithContext(adapted, universe).initialize(sourceSection);
    }

    @Override
    public boolean isTrivial() {
      return false;
    }

    @Override
    public PreevaluatedExpression copyTrivialNode() {
      throw new UnsupportedOperationException("Block literals with context are not trivial.");
    }
  }
}
