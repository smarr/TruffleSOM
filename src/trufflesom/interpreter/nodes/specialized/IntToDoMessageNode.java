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
@Primitive(selector = "to:do:", noWrapper = true, disabled = true, inParser = false)
public abstract class IntToDoMessageNode extends TernaryExpressionNode {

  @Child private BlockDispatchNode blockNode = BlockDispatchNodeGen.create();

  @Specialization
  public final long doIntToDo(final long receiver, final long limit, final SBlock block) {
    try {
      doLooping(receiver, limit, block);
    } finally {
      if (CompilerDirectives.inInterpreter() && (limit - receiver) > 0) {
        reportLoopCount(limit - receiver);
      }
    }
    return receiver;
  }

  @Specialization
  public final long doIntToDo(final long receiver, final double dLimit, final SBlock block) {
    long limit = (long) dLimit;
    return doIntToDo(receiver, limit, block);
  }

  protected final void doLooping(final long receiver, final long limit, final SBlock block) {
    if (receiver <= limit) {
      blockNode.executeDispatch(new Object[] {block, receiver});
    }
    for (long i = receiver + 1; i <= limit; i++) {
      blockNode.executeDispatch(new Object[] {block, i});
    }
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
