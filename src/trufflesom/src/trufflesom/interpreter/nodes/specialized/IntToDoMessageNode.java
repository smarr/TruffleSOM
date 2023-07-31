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

import trufflesom.bdt.primitives.Primitive;
import trufflesom.interpreter.Invokable;
import trufflesom.interpreter.nodes.nary.TernaryMsgExprNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SSymbol;


@GenerateNodeFactory
@Primitive(selector = "to:do:", disabled = true, inParser = false)
public abstract class IntToDoMessageNode extends TernaryMsgExprNode {
  protected static final int LIMIT = 3;

  @Override
  public SSymbol getSelector() {
    return SymbolTable.symbolFor("to:do:");
  }

  @Specialization(guards = {"block.getMethod() == cachedMethod"}, limit = "LIMIT")
  public final long doIntCached(final long receiver, final long limit, final SBlock block,
      @Cached("block.getMethod()") final SInvokable cachedMethod,
      @Cached("create(cachedMethod.getCallTarget())") final DirectCallNode callNode) {
    try {
      doLooping(receiver, limit, block, callNode);
    } finally {
      if (CompilerDirectives.inInterpreter() && (limit - receiver) > 0) {
        reportLoopCount(limit - receiver);
      }
    }
    return receiver;
  }

  @Specialization(replaces = "doIntCached")
  public final long doIntUncached(final long receiver, final long limit, final SBlock block,
      @Shared("all") @Cached final IndirectCallNode callNode) {
    try {
      doLooping(receiver, limit, block, callNode, block.getMethod().getCallTarget());
    } finally {
      if (CompilerDirectives.inInterpreter() && (limit - receiver) > 0) {
        reportLoopCount(limit - receiver);
      }
    }
    return receiver;
  }

  @Specialization(guards = {"block.getMethod() == cachedMethod"}, limit = "LIMIT")
  public final long doDoubleCached(final long receiver, final double dLimit,
      final SBlock block,
      @Cached("block.getMethod()") final SInvokable cachedMethod,
      @Cached("create(cachedMethod.getCallTarget())") final DirectCallNode callNode) {
    long limit = (long) dLimit;
    return doIntCached(receiver, limit, block, cachedMethod, callNode);
  }

  @Specialization(replaces = "doDoubleCached")
  public final long doDoubleUncached(final long receiver, final double dLimit,
      final SBlock block,
      @Shared("all") @Cached final IndirectCallNode callNode) {
    long limit = (long) dLimit;
    return doIntUncached(receiver, limit, block, callNode);
  }

  protected static final void doLooping(final long receiver, final long limit,
      final SBlock block, final DirectCallNode callNode) {
    if (receiver <= limit) {
      callNode.call(new Object[] {block, receiver});
    }
    for (long i = receiver + 1; i <= limit; i++) {
      callNode.call(new Object[] {block, i});
    }
  }

  protected static final void doLooping(final long receiver, final long limit,
      final SBlock block, final IndirectCallNode callNode, final CallTarget ct) {
    if (receiver <= limit) {
      callNode.call(ct, new Object[] {block, receiver});
    }
    for (long i = receiver + 1; i <= limit; i++) {
      callNode.call(ct, new Object[] {block, i});
    }
  }

  @Specialization(guards = {"block.getMethod() == cachedMethod"}, limit = "LIMIT")
  public final double doDoubleDoubleCached(final double receiver, final double limit,
      final SBlock block,
      @Cached("block.getMethod()") final SInvokable cachedMethod,
      @Cached("create(cachedMethod.getCallTarget())") final DirectCallNode callNode) {
    try {
      if (receiver <= limit) {
        callNode.call(new Object[] {block, receiver});
      }
      for (double i = receiver + 1.0; i <= limit; i += 1.0) {
        callNode.call(new Object[] {block, i});
      }
    } finally {
      if (CompilerDirectives.inInterpreter() && (limit - receiver) > 0) {
        reportLoopCount((int) (limit - receiver));
      }
    }
    return receiver;
  }

  @Specialization(replaces = "doDoubleDoubleCached")
  public final double doDoubleDoubleUncached(final double receiver, final double limit,
      final SBlock block,
      @Shared("all") @Cached final IndirectCallNode callNode) {
    CallTarget ct = block.getMethod().getCallTarget();
    try {
      if (receiver <= limit) {
        callNode.call(ct, new Object[] {block, receiver});
      }
      for (double i = receiver + 1.0; i <= limit; i += 1.0) {
        callNode.call(ct, new Object[] {block, i});
      }
    } finally {
      if (CompilerDirectives.inInterpreter() && (limit - receiver) > 0) {
        reportLoopCount((int) (limit - receiver));
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
