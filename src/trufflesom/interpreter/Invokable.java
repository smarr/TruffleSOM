package trufflesom.interpreter;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;

import bd.primitives.nodes.PreevaluatedExpression;
import trufflesom.compiler.MethodGenerationContext;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable.SMethod;


public abstract class Invokable extends AbstractInvokable {
  protected final String name;
  protected final Source source;
  protected final long   sourceCoord;

  protected SClass holder;

  protected Invokable(final String name, final Source source, final long sourceCoord,
      final FrameDescriptor frameDescriptor) {
    super(SomLanguage.getCurrent(), frameDescriptor);
    this.name = name;
    this.source = source;
    this.sourceCoord = sourceCoord;
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

  public SClass getHolder() {
    return holder;
  }

  @Override
  public void setHolder(final SClass holder) {
    this.holder = holder;
  }

  @Override
  public abstract boolean isTrivial();

  public abstract PreevaluatedExpression copyTrivialNode();
}
