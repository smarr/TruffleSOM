package som.interpreter;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;

import som.compiler.MethodGenerationContext;
import som.interpreter.nodes.ExpressionNode;
import som.vmobjects.SInvokable.SMethod;


public abstract class Invokable extends RootNode {
  protected final String        name;
  protected final SourceSection sourceSection;

  @Child protected ExpressionNode expressionOrSequence;

  protected final ExpressionNode uninitializedBody;

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

  /** Inline invokable into the lexical context of the given builder. */
  public abstract ExpressionNode inline(MethodGenerationContext mgenc, SMethod outer);

  @Override
  public final boolean isCloningAllowed() {
    return true;
  }

  public final RootCallTarget createCallTarget() {
    return Truffle.getRuntime().createCallTarget(this);
  }

  public abstract void propagateLoopCountThroughoutLexicalScope(long count);
}
