package trufflesom.primitives.reflection;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.bdt.inlining.nodes.WithSource;
import trufflesom.bdt.primitives.nodes.PreevaluatedExpression;
import trufflesom.bdt.source.SourceCoordinate;
import trufflesom.interpreter.Types;
import trufflesom.interpreter.nodes.AbstractMessageSendNode;
import trufflesom.interpreter.nodes.MessageSendNode;
import trufflesom.primitives.arrays.ToArgumentsArrayNode;
import trufflesom.primitives.arrays.ToArgumentsArrayNodeFactory;
import trufflesom.tools.nodestats.Tags.AnyNode;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SSymbol;


@GenerateWrapper
public abstract class AbstractSymbolDispatch extends Node
    implements WithSource, InstrumentableNode {
  public static final int INLINE_CACHE_SIZE = 6;

  private final long sourceCoord;

  public AbstractSymbolDispatch(final long coord) {
    assert coord != 0;
    this.sourceCoord = coord;
  }

  protected AbstractSymbolDispatch(final AbstractSymbolDispatch wrapped) {
    this.sourceCoord = wrapped.sourceCoord;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Node> T initialize(final long srcCoord) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getSourceCoordinate() {
    return sourceCoord;
  }

  @Override
  public Source getSource() {
    return ((WithSource) getParent()).getSource();
  }

  @Override
  public boolean hasSource() {
    return false;
  }

  @Override
  public final SourceSection getSourceSection() {
    return SourceCoordinate.createSourceSection(this, sourceCoord);
  }

  public abstract Object executeDispatch(VirtualFrame frame, Object receiver,
      SSymbol selector, Object argsArr);

  @NeverDefault
  protected final AbstractMessageSendNode createForPerformNodes(final SSymbol selector) {
    return MessageSendNode.createForPerformNodes(selector, sourceCoord);
  }

  @NeverDefault
  public static final ToArgumentsArrayNode createArgArrayNode() {
    return ToArgumentsArrayNodeFactory.create(null, null);
  }

  @Override
  public boolean isInstrumentable() {
    return true;
  }

  @Override
  public WrapperNode createWrapper(final ProbeNode probe) {
    return new AbstractSymbolDispatchWrapper(this, this, probe);
  }

  @Override
  public boolean hasTag(final Class<? extends Tag> tag) {
    if (tag == AnyNode.class) {
      return true;
    }
    return false;
  }

  @Specialization(limit = "INLINE_CACHE_SIZE",
      guards = {"selector == cachedSelector", "argsArr == null"})
  @SuppressWarnings("unused")
  public Object doCachedWithoutArgArr(final VirtualFrame frame,
      final Object receiver, @SuppressWarnings("unused") final SSymbol selector,
      @SuppressWarnings("unused") final Object argsArr,
      @SuppressWarnings("unused") @Cached("selector") final SSymbol cachedSelector,
      @Cached("createForPerformNodes(selector)") final AbstractMessageSendNode cachedSend) {
    Object[] arguments = {receiver};

    PreevaluatedExpression realCachedSend = cachedSend;
    return realCachedSend.doPreEvaluated(frame, arguments);
  }

  @Specialization(limit = "INLINE_CACHE_SIZE", guards = "selector == cachedSelector")
  @SuppressWarnings("unused")
  public Object doCached(final VirtualFrame frame,
      final Object receiver, @SuppressWarnings("unused") final SSymbol selector,
      final SArray argsArr,
      @SuppressWarnings("unused") @Cached("selector") final SSymbol cachedSelector,
      @Cached("createForPerformNodes(selector)") final AbstractMessageSendNode cachedSend,
      @Shared("arg") @Cached("createArgArrayNode()") final ToArgumentsArrayNode toArgArray) {
    Object[] arguments = toArgArray.executedEvaluated(frame, argsArr, receiver);

    PreevaluatedExpression realCachedSend = cachedSend;
    return realCachedSend.doPreEvaluated(frame, arguments);
  }

  @TruffleBoundary
  @Specialization(replaces = "doCachedWithoutArgArr", guards = "argsArr == null")
  public Object doUncached(final Object receiver, final SSymbol selector,
      @SuppressWarnings("unused") final Object argsArr,
      @Shared("indirect") @Cached final IndirectCallNode call) {
    SInvokable invokable = Types.getClassOf(receiver).lookupInvokable(selector);

    Object[] arguments = {receiver};

    return call.call(invokable.getCallTarget(), arguments);
  }

  @TruffleBoundary
  @Specialization(replaces = "doCached")
  public Object doUncached(final Object receiver, final SSymbol selector, final SArray argsArr,
      @Shared("indirect") @Cached final IndirectCallNode call,
      @Shared("arg") @Cached("createArgArrayNode()") final ToArgumentsArrayNode toArgArray) {
    SInvokable invokable = Types.getClassOf(receiver).lookupInvokable(selector);

    Object[] arguments = toArgArray.executedEvaluated(null, argsArr, receiver);

    return call.call(invokable.getCallTarget(), arguments);
  }
}
