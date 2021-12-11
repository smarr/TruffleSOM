package trufflesom.interpreter;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import bd.inlining.nodes.WithSource;
import bd.primitives.nodes.PreevaluatedExpression;
import bd.source.SourceCoordinate;
import trufflesom.compiler.MethodGenerationContext;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable.SMethod;


public abstract class Invokable extends RootNode implements WithSource {
  protected final String name;
  protected final Source source;
  protected final long   sourceCoord;

  @Child protected ExpressionNode expressionOrSequence;

  protected final ExpressionNode uninitializedBody;

  protected SClass holder;

  protected Invokable(final String name, final Source source, final long sourceCoord,
      final FrameDescriptor frameDescriptor,
      final ExpressionNode expressionOrSequence,
      final ExpressionNode uninitialized) {
    super(SomLanguage.getCurrent(), frameDescriptor);
    this.name = name;
    this.source = source;
    this.sourceCoord = sourceCoord;
    this.uninitializedBody = uninitialized;
    this.expressionOrSequence = expressionOrSequence;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Invokable initialize(final long sourceCoord) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Source getSource() {
    return source;
  }

  @Override
  public boolean hasSource() {
    return true;
  }

  @Override
  public long getSourceCoordinate() {
    return sourceCoord;
  }

  @Override
  public SourceSection getSourceSection() {
    return SourceCoordinate.createSourceSection(source, sourceCoord);
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
