package trufflesom.interpreter;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;

import bdt.inlining.nodes.WithSource;
import bdt.primitives.nodes.PreevaluatedExpression;
import bdt.source.SourceCoordinate;
import trufflesom.compiler.MethodGenerationContext;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable.SMethod;


public abstract class Invokable extends AbstractInvokable {
  protected final String name;

  protected SClass holder;

  protected Invokable(final String name, final Source source, final long sourceCoord,
      final FrameDescriptor frameDescriptor) {
    super(frameDescriptor, source, sourceCoord);
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  /** Inline invokable into the lexical context of the target method generation context. */
  @Override
  public abstract ExpressionNode inline(MethodGenerationContext targetMgenc,
      SMethod toBeInlined);

  @Override
  public final boolean isCloningAllowed() {
    return true;
  }

  @Override
  protected boolean isCloneUninitializedSupported() {
    return true;
  }

  @Override
  protected RootNode cloneUninitialized() {
    return (RootNode) deepCopy();
  }

  public abstract void propagateLoopCountThroughoutLexicalScope(long count);

  public SClass getHolder() {
    return holder;
  }

  @Override
  public void setHolder(final SClass holder) {
    this.holder = holder;
  }
}
