package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

import bdt.primitives.Primitive;
import trufflesom.interpreter.Invokable;
import trufflesom.interpreter.nodes.nary.TernaryMsgExprNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SSymbol;


@GenerateNodeFactory
@Primitive(selector = "downTo:do:", disabled = true, inParser = false)
public abstract class IntDownToDoMessageNode extends TernaryMsgExprNode {
  protected static final int LIMIT = 3;

  @Override
  public SSymbol getSelector() {
    return SymbolTable.symbolFor("downTo:do:");
  }

  @Specialization(guards = {"block.getMethod() == cachedMethod"}, limit = "LIMIT")
  public final long doIntCached(final long receiver, final long limit, final SBlock block,
      @Cached("block.getMethod()") final SInvokable cachedMethod,
      @Cached("create(cachedMethod.getCallTarget())") final DirectCallNode callNode) {
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
      @Shared("all") @Cached final IndirectCallNode callNode) {
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

  @Specialization(guards = {"block.getMethod() == cachedMethod"}, limit = "LIMIT")
  public final long doDoubleCached(final long receiver, final double limit, final SBlock block,
      @Cached("block.getMethod()") final SInvokable cachedMethod,
      @Cached("create(cachedMethod.getCallTarget())") final DirectCallNode callNode) {
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
      @Shared("all") @Cached final IndirectCallNode callNode) {
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
