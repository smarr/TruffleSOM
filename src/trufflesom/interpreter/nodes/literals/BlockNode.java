package trufflesom.interpreter.nodes.literals;

import java.util.ArrayList;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;

import bd.inlining.ScopeAdaptationVisitor;
import trufflesom.compiler.MethodGenerationContext;
import trufflesom.compiler.Variable;
import trufflesom.compiler.Variable.Argument;
import trufflesom.interpreter.Method;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SInvokable.SMethod;


public class BlockNode extends LiteralNode {

  protected final SMethod blockMethod;
  protected final Method  invokable;

  @CompilationFinal protected DynamicObject blockClass;

  protected final Universe universe;

  public BlockNode(final SMethod blockMethod, final Method invokable,
      final Universe universe) {
    this.blockMethod = blockMethod;
    this.invokable = invokable;
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
        inliner.outerScopeChanged());
    SMethod method = (SMethod) Universe.newMethod(blockMethod.getSignature(), adapted, false,
        blockMethod.getEmbeddedBlocks(), blockIvk.getSourceSection());
    replace(createNode(method, adapted));
  }

  protected BlockNode createNode(final SMethod method, final Method adapted) {
    return new BlockNode(method, adapted, universe).initialize(sourceSection);
  }

  @Override
  public ExpressionNode inline(final MethodGenerationContext mgenc) {
    return blockMethod.getInvokable().inline(mgenc, blockMethod);
  }

  public static final class BlockNodeWithContext extends BlockNode {

    public BlockNodeWithContext(final SMethod blockMethod, final Method method,
        final Universe universe) {
      super(blockMethod, method, universe);
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
    protected BlockNode createNode(final SMethod method, final Method adapted) {
      return new BlockNodeWithContext(method, adapted, universe).initialize(sourceSection);
    }
  }
}
