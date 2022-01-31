package trufflesom.interpreter;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.nodes.Node;
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


public abstract class AbstractInvokable extends RootNode implements WithSource {
  protected final Source source;
  protected final long   sourceCoord;

  protected AbstractInvokable(final FrameDescriptor frameDescriptor, final Source source,
      final long sourceCoord) {
    super(SomLanguage.getCurrent(), frameDescriptor);
    this.source = source;
    this.sourceCoord = sourceCoord;
  }

  /** Inline invokable into the lexical context of the target method generation context. */
  public ExpressionNode inline(final MethodGenerationContext targetMgenc,
      final SMethod toBeInlined) {
    CompilerDirectives.transferToInterpreterAndInvalidate();
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isTrivial() {
    return false;
  }

  public PreevaluatedExpression copyTrivialNode() {
    CompilerDirectives.transferToInterpreterAndInvalidate();
    throw new UnsupportedOperationException();
  }

  public void setHolder(final SClass holder) { /* no op */ }

  @Override
  public final <T extends Node> T initialize(final long sourceCoord) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final Source getSource() {
    return source;
  }

  @Override
  public final boolean hasSource() {
    return true;
  }

  @Override
  public final long getSourceCoordinate() {
    return sourceCoord;
  }

  @Override
  public final SourceSection getSourceSection() {
    return SourceCoordinate.createSourceSection(source, sourceCoord);
  }
}
