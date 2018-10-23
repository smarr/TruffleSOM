package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

import bd.primitives.Primitive;
import trufflesom.interpreter.Invokable;
import trufflesom.interpreter.nodes.nary.TernaryExpressionNode;
import trufflesom.interpreter.nodes.specialized.IntToDoMessageNode.ToDoSplzr;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SInvokable;


@GenerateNodeFactory
@Primitive(selector = "downTo:do:", noWrapper = true, disabled = true,
    specializer = ToDoSplzr.class, inParser = false, requiresArguments = true)
public abstract class IntDownToDoMessageNode extends TernaryExpressionNode {

  private final SInvokable      blockMethod;
  @Child private DirectCallNode valueSend;

  public IntDownToDoMessageNode(final Object[] args) {
    blockMethod = ((SBlock) args[2]).getMethod();
    valueSend = Truffle.getRuntime().createDirectCallNode(
        blockMethod.getCallTarget());
  }

  protected final boolean isSameBlockLong(final SBlock block) {
    return block.getMethod() == blockMethod;
  }

  @Specialization(guards = "isSameBlockLong(block)")
  public final long doIntDownToDo(final long receiver, final long limit, final SBlock block) {
    try {
      if (receiver >= limit) {
        valueSend.call(new Object[] {block, receiver});
      }
      for (long i = receiver - 1; i >= limit; i--) {
        valueSend.call(new Object[] {block, i});
      }
    } finally {
      if (CompilerDirectives.inInterpreter() && (receiver - limit) > 0) {
        reportLoopCount(receiver - limit);
      }
    }
    return receiver;
  }

  protected final boolean isSameBlockDouble(final SBlock block) {
    return block.getMethod() == blockMethod;
  }

  @Specialization(guards = "isSameBlockDouble(block)")
  public final long doIntDownToDo(final long receiver, final double limit,
      final SBlock block) {
    try {
      if (receiver >= limit) {
        valueSend.call(new Object[] {block, receiver});
      }
      for (long i = receiver - 1; i >= limit; i--) {
        valueSend.call(new Object[] {block, i});
      }
    } finally {
      if (CompilerDirectives.inInterpreter() && (receiver - (int) limit) > 0) {
        reportLoopCount(receiver - (int) limit);
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
