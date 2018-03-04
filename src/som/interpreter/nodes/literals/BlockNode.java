package som.interpreter.nodes.literals;

import java.util.ArrayList;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;

import bd.inlining.ScopeAdaptationVisitor;
import som.compiler.MethodGenerationContext;
import som.compiler.Variable;
import som.compiler.Variable.Argument;
import som.interpreter.Method;
import som.interpreter.nodes.ExpressionNode;
import som.vm.Universe;
import som.vmobjects.SBlock;
import som.vmobjects.SClass;
import som.vmobjects.SInvokable.SMethod;


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
    return Universe.newBlock(blockMethod, blockClass, null);
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
    Method adapted = blockIvk.cloneAndAdaptAfterScopeChange(
        inliner.getScope(blockIvk), inliner.contextLevel + 1, true,
        inliner.someOuterScopeIsMerged());
    SMethod method = (SMethod) Universe.newMethod(blockMethod.getSignature(), adapted, false,
        blockMethod.getEmbeddedBlocks());
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
      return Universe.newBlock(blockMethod, blockClass, frame.materialize());
    }

    @Override
    protected BlockNode createNode(final SMethod adapted) {
      return new BlockNodeWithContext(adapted, universe).initialize(sourceSection);
    }
  }
}
