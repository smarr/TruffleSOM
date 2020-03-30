package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

import bd.primitives.Primitive;
import trufflesom.interpreter.Invokable;
import trufflesom.interpreter.nodes.dispatch.BlockDispatchNode;
import trufflesom.interpreter.nodes.dispatch.BlockDispatchNodeGen;
import trufflesom.interpreter.nodes.nary.TernaryExpressionNode;
import trufflesom.vmobjects.SBlock;


@GenerateNodeFactory
@Primitive(selector = "downTo:do:", noWrapper = true, disabled = true, inParser = false)
public abstract class IntDownToDoMessageNode extends TernaryExpressionNode {

  @Child private BlockDispatchNode blockNode = BlockDispatchNodeGen.create();

  @Specialization
  public final long doIntDownToDo(final long receiver, final long limit, final SBlock block) {
    try {
      if (receiver >= limit) {
        blockNode.executeDispatch(new Object[] {block, receiver});
      }
      for (long i = receiver - 1; i >= limit; i--) {
        blockNode.executeDispatch(new Object[] {block, i});
      }
    } finally {
      if (CompilerDirectives.inInterpreter() && (receiver - limit) > 0) {
        reportLoopCount(receiver - limit);
      }
    }
    return receiver;
  }

  @Specialization
  public final long doIntDownToDo(final long receiver, final double limit,
      final SBlock block) {
    try {
      if (receiver >= limit) {
        blockNode.executeDispatch(new Object[] {block, receiver});
      }
      for (long i = receiver - 1; i >= limit; i--) {
        blockNode.executeDispatch(new Object[] {block, i});
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
