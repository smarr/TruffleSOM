package trufflesom.primitives.reflection;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;

import trufflesom.interpreter.Types;
import trufflesom.interpreter.nodes.MessageSendNode;
import trufflesom.interpreter.nodes.MessageSendNode.AbstractMessageSendNode;
import trufflesom.interpreter.nodes.PreevaluatedExpression;
import trufflesom.primitives.arrays.ToArgumentsArrayNode;
import trufflesom.primitives.arrays.ToArgumentsArrayNodeFactory;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SSymbol;


public abstract class AbstractSymbolDispatch extends Node {
  public static final int INLINE_CACHE_SIZE = 6;

  protected final Universe universe;

  public AbstractSymbolDispatch(final Universe universe) {
    this.universe = universe;
  }

  public abstract Object executeDispatch(VirtualFrame frame, Object receiver,
      SSymbol selector, Object argsArr);

  public static final AbstractMessageSendNode createForPerformNodes(final SSymbol selector,
      final Universe universe) {
    return MessageSendNode.createForPerformNodes(selector, universe);
  }

  public static final ToArgumentsArrayNode createArgArrayNode() {
    return ToArgumentsArrayNodeFactory.create(null, null);
  }

  @Specialization(limit = "INLINE_CACHE_SIZE",
      guards = {"selector == cachedSelector", "argsArr == null"})
  public Object doCachedWithoutArgArr(final VirtualFrame frame,
      final Object receiver, final SSymbol selector, final Object argsArr,
      @Cached("selector") final SSymbol cachedSelector,
      @Cached("createForPerformNodes(selector, universe)") final AbstractMessageSendNode cachedSend) {
    Object[] arguments = {receiver};

    PreevaluatedExpression realCachedSend = cachedSend;
    return realCachedSend.doPreEvaluated(frame, arguments);
  }

  @Specialization(limit = "INLINE_CACHE_SIZE", guards = "selector == cachedSelector")
  public Object doCached(final VirtualFrame frame,
      final Object receiver, final SSymbol selector, final SArray argsArr,
      @Cached("selector") final SSymbol cachedSelector,
      @Cached("createForPerformNodes(selector, universe)") final AbstractMessageSendNode cachedSend,
      @Cached("createArgArrayNode()") final ToArgumentsArrayNode toArgArray) {
    Object[] arguments = toArgArray.executedEvaluated(argsArr, receiver);

    PreevaluatedExpression realCachedSend = cachedSend;
    return realCachedSend.doPreEvaluated(frame, arguments);
  }

  @Specialization(replaces = "doCachedWithoutArgArr", guards = "argsArr == null")
  public Object doUncached(final Object receiver, final SSymbol selector, final Object argsArr,
      @Cached("create()") final IndirectCallNode call) {
    SInvokable invokable = Types.getClassOf(receiver, universe).lookupInvokable(selector);

    Object[] arguments = {receiver};

    return call.call(invokable.getCallTarget(), arguments);
  }

  @Specialization(replaces = "doCached")
  public Object doUncached(final Object receiver, final SSymbol selector, final SArray argsArr,
      @Cached("create()") final IndirectCallNode call,
      @Cached("createArgArrayNode()") final ToArgumentsArrayNode toArgArray) {
    SInvokable invokable = Types.getClassOf(receiver, universe).lookupInvokable(selector);

    Object[] arguments = toArgArray.executedEvaluated(argsArr, receiver);

    return call.call(invokable.getCallTarget(), arguments);
  }
}
