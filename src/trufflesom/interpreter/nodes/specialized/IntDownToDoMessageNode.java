package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

import bd.primitives.Primitive;
import trufflesom.interpreter.Invokable;
import trufflesom.interpreter.nodes.nary.TernaryExpressionNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SInvokable;


@GenerateNodeFactory
@Primitive(selector = "downTo:do:", disabled = true, inParser = false)
public abstract class IntDownToDoMessageNode extends TernaryExpressionNode {

  protected static final DirectCallNode createCallNode(final CallTarget ct) {
    return Truffle.getRuntime().createDirectCallNode(ct);
  }

  protected static final IndirectCallNode createIndirectCall() {
    return Truffle.getRuntime().createIndirectCallNode();
  }

  @Specialization(guards = {"block.getMethod() == cachedMethod"})
  public final long doIntCached(final long receiver, final long limit, final SBlock block,
      @Cached("block.getMethod()") final SInvokable cachedMethod,
      @Cached("createCallNode(cachedMethod.getCallTarget())") final DirectCallNode callNode) {
    try {
      if (receiver >= limit) {
        callNode.call(new Object[] {block, receiver});
      }
      for (long i = receiver - 1; i >= limit; i--) {
        callNode.call(new Object[] {block, i});
      }
    } finally {
      if (CompilerDirectives.inInterpreter() && (receiver - limit) > 0) {
        reportLoopCount(receiver - limit);
      }
    }
    return receiver;
  }

  @Specialization(replaces = "doIntCached")
  public final long doIntUncached(final long receiver, final long limit, final SBlock block,
      @Cached("createIndirectCall()") final IndirectCallNode callNode) {
    CallTarget ct = block.getMethod().getCallTarget();
    try {
      if (receiver >= limit) {
        callNode.call(ct, new Object[] {block, receiver});
      }
      for (long i = receiver - 1; i >= limit; i--) {
        callNode.call(ct, new Object[] {block, i});
      }
    } finally {
      if (CompilerDirectives.inInterpreter() && (receiver - limit) > 0) {
        reportLoopCount(receiver - limit);
      }
    }
    return receiver;
  }

  @Specialization(guards = {"block.getMethod() == cachedMethod"})
  public final long doDoubleCached(final long receiver, final double limit, final SBlock block,
      @Cached("block.getMethod()") final SInvokable cachedMethod,
      @Cached("createCallNode(cachedMethod.getCallTarget())") final DirectCallNode callNode) {
    try {
      if (receiver >= limit) {
        callNode.call(new Object[] {block, receiver});
      }
      for (long i = receiver - 1; i >= limit; i--) {
        callNode.call(new Object[] {block, i});
      }
    } finally {
      if (CompilerDirectives.inInterpreter() && (receiver - (int) limit) > 0) {
        reportLoopCount(receiver - (int) limit);
      }
    }
    return receiver;
  }

  @Specialization(replaces = "doDoubleCached")
  public final double doDoubleUncached(final double receiver, final double limit,
      final SBlock block,
      @Cached("createIndirectCall()") final IndirectCallNode callNode) {
    CallTarget ct = block.getMethod().getCallTarget();
    try {
      if (receiver >= limit) {
        callNode.call(ct, new Object[] {block, receiver});
      }
      double i = receiver - 1.0;
      while (i >= limit) {
        callNode.call(ct, new Object[] {block, i});
        i -= 1.0;
      }
    } finally {
      int loopCount = (int) (receiver - limit);
      if (CompilerDirectives.inInterpreter() && loopCount > 0) {
        reportLoopCount(loopCount);
      }
    }
    return receiver;
  }

  @Fallback
  public final Object makeGeneric(final VirtualFrame frame, final Object receiver,
      final Object limit, final Object block) {
    return makeGenericSend(SymbolTable.symbolFor("downTo:do:")).doPreEvaluated(frame,
        new Object[] {receiver, limit, block});
  }

  private void reportLoopCount(final long count) {
    CompilerAsserts.neverPartOfCompilation("reportLoopCount");
    Node current = getParent();
    while (current != null && !(current instanceof RootNode)) {
      current = current.getParent();
    }
    if (current != null) {
      ((Invokable) current).propagateLoopCountThroughoutLexicalScope(count);
    }
  }
}
