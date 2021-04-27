package trufflesom.interpreter;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;

import bd.primitives.nodes.PreevaluatedExpression;
import trufflesom.compiler.MethodGenerationContext;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable.SMethod;


public abstract class Invokable extends RootNode {
  protected final String        name;
  protected final SourceSection sourceSection;

  @Child protected ExpressionNode expressionOrSequence;

  protected final ExpressionNode uninitializedBody;

  @CompilationFinal protected SClass holder;

  protected Invokable(final String name, final SourceSection sourceSection,
      final FrameDescriptor frameDescriptor,
      final ExpressionNode expressionOrSequence,
      final ExpressionNode uninitialized, final SomLanguage lang) {
    super(lang, frameDescriptor);
    this.name = name;
    this.sourceSection = sourceSection;
    this.uninitializedBody = uninitialized;
    this.expressionOrSequence = expressionOrSequence;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public SourceSection getSourceSection() {
    return sourceSection;
  }

  @Override
  public final Object execute(final VirtualFrame frame) {
    return expressionOrSequence.executeGeneric(frame);
  }

  /** Inline invokable into the lexical context of the target method generation context. */
  public abstract ExpressionNode inline(MethodGenerationContext targetMgenc,
      SMethod toBeInlined);

  @Override
  public final boolean isCloningAllowed() {
    return true;
  }

  public abstract void propagateLoopCountThroughoutLexicalScope(long count);

  public SClass getHolder() {
    return holder;
  }

  public void setHolder(final SClass holder) {
    this.holder = holder;
  }

  @Override
  public boolean isTrivial() {
    return expressionOrSequence.isTrivial();
  }

  public PreevaluatedExpression copyTrivialNode() {
    return expressionOrSequence.copyTrivialNode();
  }
}
