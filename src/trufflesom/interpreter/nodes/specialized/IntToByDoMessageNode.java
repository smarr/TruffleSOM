package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;

import bdt.primitives.Primitive;
import trufflesom.interpreter.Invokable;
import trufflesom.interpreter.nodes.nary.QuaternaryMsgExprNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SSymbol;


@GenerateNodeFactory
@Primitive(selector = "to:by:do:", disabled = true, requiresArguments = true)
public abstract class IntToByDoMessageNode extends QuaternaryMsgExprNode {

  protected final SInvokable    blockMethod;
  @Child private DirectCallNode valueSend;

  public IntToByDoMessageNode(final Object[] args) {
    blockMethod = ((SBlock) args[3]).getMethod();
    valueSend = Truffle.getRuntime().createDirectCallNode(
        blockMethod.getCallTarget());
  }

  @Override
  public SSymbol getSelector() {
    return SymbolTable.symbolFor("to:by:do:");
  }

  @Specialization(guards = "block.getMethod() == blockMethod")
  public final long doIntToByDo(final long receiver,
      final long limit, final long step, final SBlock block) {
    try {
      if (receiver <= limit) {
        valueSend.call(new Object[] {block, receiver});
      }
      for (long i = receiver + step; i <= limit; i += step) {
        valueSend.call(new Object[] {block, i});
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount(limit - receiver);
      }
    }
    return receiver;
  }

  @Specialization(guards = "block.getMethod() == blockMethod")
  public final long doIntToByDo(final long receiver,
      final double limit, final long step, final SBlock block) {
    try {
      if (receiver <= limit) {
        valueSend.call(new Object[] {block, receiver});
      }
      for (long i = receiver + step; i <= limit; i += step) {
        valueSend.call(new Object[] {block, i});
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount((long) limit - receiver);
      }
    }
    return receiver;
  }

  protected final void reportLoopCount(final long count) {
    CompilerAsserts.neverPartOfCompilation("reportLoopCount");
    Node current = getRootNode();

    if (current != null) {
      ((Invokable) current).propagateLoopCountThroughoutLexicalScope(Math.abs(count));
    }
  }
}
