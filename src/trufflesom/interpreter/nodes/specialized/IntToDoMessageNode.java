package som.interpreter.nodes.specialized;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

import som.interpreter.Invokable;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.interpreter.nodes.specialized.IntToDoMessageNode.ToDoSplzr;
import som.primitives.Primitive;
import som.primitives.Specializer;
import som.vm.Universe;
import som.vm.VmSettings;
import som.vmobjects.SBlock;
import som.vmobjects.SInvokable;


@GenerateNodeFactory
@Primitive(selector = "to:do:", noWrapper = true, disabled = true,
    specializer = ToDoSplzr.class, inParser = false)
public abstract class IntToDoMessageNode extends TernaryExpressionNode {

  public static class ToDoSplzr extends Specializer<IntToDoMessageNode> {
    public ToDoSplzr(final Primitive prim, final NodeFactory<IntToDoMessageNode> fact,
        final Universe universe) {
      super(prim, fact, universe);
    }

    @Override
    public boolean matches(final Object[] args,
        final ExpressionNode[] argNodes) {
      return !VmSettings.DYNAMIC_METRICS && args[0] instanceof Long &&
          (args[1] instanceof Long || args[1] instanceof Double) &&
          args[2] instanceof SBlock;
    }
  }

  private final SInvokable      blockMethod;
  @Child private DirectCallNode valueSend;

  public IntToDoMessageNode(final ExpressionNode orignialNode,
      final SBlock block) {
    super(orignialNode.getSourceSection());
    blockMethod = block.getMethod();
    valueSend = Truffle.getRuntime().createDirectCallNode(
        blockMethod.getCallTarget());
  }

  public IntToDoMessageNode(final IntToDoMessageNode node) {
    super(node.getSourceSection());
    this.blockMethod = node.blockMethod;
    this.valueSend = node.valueSend;
  }

  protected final boolean isSameBlockLong(final SBlock block) {
    return block.getMethod() == blockMethod;
  }

  @Specialization(guards = "isSameBlockLong(block)")
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

  protected final boolean isSameBlockDouble(final SBlock block) {
    return block.getMethod() == blockMethod;
  }

  @Specialization(guards = "isSameBlockDouble(block)")
  public final long doIntToDo(final long receiver, final double dLimit, final SBlock block) {
    long limit = (long) dLimit;
    try {
      doLooping(receiver, limit, block);
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount((int) limit - receiver);
      }
    }
    return receiver;
  }

  protected void doLooping(final long receiver, final long limit, final SBlock block) {
    if (receiver <= limit) {
      valueSend.call(new Object[] {block, receiver});
    }
    for (long i = receiver + 1; i <= limit; i++) {
      valueSend.call(new Object[] {block, i});
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
