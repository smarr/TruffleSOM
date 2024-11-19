package trufflesom.interpreter.nodes.literals;

import java.util.ArrayList;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.bdt.inlining.ScopeAdaptationVisitor;
import trufflesom.bdt.primitives.nodes.PreevaluatedExpression;
import trufflesom.compiler.MethodGenerationContext;
import trufflesom.compiler.Variable;
import trufflesom.compiler.Variable.Argument;
import trufflesom.interpreter.Method;
import trufflesom.interpreter.Method.OpBuilder;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.vm.Classes;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable.SMethod;


public class BlockNode extends LiteralNode {

  protected final SMethod            blockMethod;
  @CompilationFinal protected SClass blockClass;

  protected final boolean reliesOnOuterFrameDescriptors;

  public BlockNode(final SMethod blockMethod, final boolean reliesOnOuterFrameDescriptors) {
    this.blockMethod = blockMethod;
    this.reliesOnOuterFrameDescriptors = reliesOnOuterFrameDescriptors;
  }

  public SMethod getMethod() {
    return blockMethod;
  }

  @Override
  public boolean isTrivial() {
    return false;
  }

  @Override
  public PreevaluatedExpression copyTrivialNode() {
    return null;
  }

  @Override
  public AbstractDispatchNode asDispatchNode(final Object rcvr, final Source source,
      final AbstractDispatchNode next) {
    return null;
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
    blockClass = Classes.getBlockClass(blockMethod.getNumberOfArguments());
  }

  @Override
  public Object doPreEvaluated(final VirtualFrame frame, final Object[] arguments) {
    if (blockClass == null) {
      CompilerDirectives.transferToInterpreter();
      setBlockClass();
    }
    return new SBlock(blockMethod, blockClass, null);
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    if (blockClass == null) {
      CompilerDirectives.transferToInterpreter();
      setBlockClass();
    }
    return new SBlock(blockMethod, blockClass, null);
  }

  @Override
  public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
    if (!inliner.outerScopeChanged() && !reliesOnOuterFrameDescriptors) {
      return;
    }

    Method blockIvk = (Method) blockMethod.getInvokable();
    Method adapted = blockIvk.cloneAndAdaptAfterScopeChange(null, inliner.getScope(blockIvk),
        inliner.contextLevel + 1, true, inliner.outerScopeChanged(),
        inliner.isSplittingOperation);

    if (inliner.isSplittingOperation) {
      // TODO: figure out whether we can do something better than passing null here.
      // Just giving `blockMethod.getEmbeddedBlocks()` would be wrong, since it's the wrong
      // versions of the blocks
      SMethod splitMethod = new SMethod(blockMethod.getSignature(), adapted,
          null /* TODO: adapt: blockMethod.getEmbeddedBlocks() */);
      replace(createNode(splitMethod));
    } else {
      blockMethod.updateAfterScopeChange(adapted);
    }
  }

  @Override
  public void constructOperation(final OpBuilder opBuilder) {
    opBuilder.dsl.beginPushBlockWithoutContextOp();
    assert blockMethod.isConverted();
    opBuilder.dsl.emitLoadConstant(blockMethod);
    opBuilder.dsl.endPushBlockWithoutContextOp();
  }

  protected BlockNode createNode(final SMethod adapted) {
    return new BlockNode(adapted, reliesOnOuterFrameDescriptors).initialize(sourceCoord);
  }

  @Override
  public ExpressionNode inline(final MethodGenerationContext mgenc) {
    return blockMethod.getInvokable().inline(mgenc, blockMethod);
  }

  public static final class BlockNodeWithContext extends BlockNode {

    public BlockNodeWithContext(final SMethod blockMethod,
        final boolean reliesOnOuterFrameDescriptors) {
      super(blockMethod, reliesOnOuterFrameDescriptors);
    }

    @Override
    public SBlock executeGeneric(final VirtualFrame frame) {
      if (blockClass == null) {
        CompilerDirectives.transferToInterpreter();
        setBlockClass();
      }
      return new SBlock(blockMethod, blockClass, frame.materialize());
    }

    @Override
    public Object doPreEvaluated(final VirtualFrame frame, final Object[] arguments) {
      if (blockClass == null) {
        CompilerDirectives.transferToInterpreter();
        setBlockClass();
      }
      return new SBlock(blockMethod, blockClass, frame.materialize());
    }

    @Override
    protected BlockNode createNode(final SMethod adapted) {
      return new BlockNodeWithContext(
          adapted, reliesOnOuterFrameDescriptors).initialize(sourceCoord);
    }

    @Override
    public void constructOperation(final OpBuilder opBuilder) {
      opBuilder.dsl.beginPushBlockWithContextOp();
      assert blockMethod.isConverted();
      opBuilder.dsl.emitLoadConstant(blockMethod);
      opBuilder.dsl.endPushBlockWithContextOp();
    }
  }
}
