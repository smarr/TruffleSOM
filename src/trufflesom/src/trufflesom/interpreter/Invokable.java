package trufflesom.interpreter;

import java.util.HashMap;

import com.oracle.truffle.api.bytecode.BytecodeLocal;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.bdt.inlining.nodes.WithSource;
import trufflesom.bdt.primitives.nodes.PreevaluatedExpression;
import trufflesom.bdt.source.SourceCoordinate;
import trufflesom.compiler.MethodGenerationContext;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.operations.SomOperations.LexicalScopeForOp;
import trufflesom.interpreter.operations.SomOperationsGen;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable.SMethod;


public abstract class Invokable extends RootNode implements WithSource {
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

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Node> T initialize(final long coord) {
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

  /** Inline invokable into the lexical context of the target method generation context. */
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

  public void setHolder(final SClass holder) {
    this.holder = holder;
  }

  @Override
  public abstract boolean isTrivial();

  public PreevaluatedExpression copyTrivialNode() {
    return null;
  }

  @SuppressWarnings("unused")
  public AbstractDispatchNode asDispatchNode(final Object rcvr,
      final AbstractDispatchNode next) {
    return null;
  }

  public abstract Invokable convertMethod(SomOperationsGen.Builder opBuilder,
      LexicalScopeForOp scope);

  public abstract HashMap<Local, BytecodeLocal> createLocals(
      SomOperationsGen.Builder opBuilder);
}
