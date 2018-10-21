package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.interpreter.Invokable;
import trufflesom.interpreter.nodes.nary.QuaternaryExpressionNode;
import trufflesom.primitives.Primitive;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SInvokable;


@GenerateNodeFactory
@Primitive(selector = "to:by:do:", disabled = true, noWrapper = true, requiresArguments = true)
public abstract class IntToByDoMessageNode extends QuaternaryExpressionNode {

  private final SInvokable      blockMethod;
  @Child private DirectCallNode valueSend;

  public IntToByDoMessageNode(final SourceSection source,
      final Object[] args) {
    super(source);
    blockMethod = ((SBlock) args[3]).getMethod();
    valueSend = Truffle.getRuntime().createDirectCallNode(
        blockMethod.getCallTarget());
  }

  public IntToByDoMessageNode(final IntToByDoMessageNode node) {
    super(node.getSourceSection());
    this.blockMethod = node.blockMethod;
    this.valueSend = node.valueSend;
  }

  protected final boolean isSameBlockLong(final SBlock block) {
    return block.getMethod() == blockMethod;
  }

  protected final boolean isSameBlockDouble(final SBlock block) {
    return block.getMethod() == blockMethod;
  }

  @Specialization(guards = "isSameBlockLong(block)")
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

  @Specialization(guards = "isSameBlockDouble(block)")
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
    Node current = getParent();
    while (current != null && !(current instanceof Invokable)) {
      current = current.getParent();
    }
    if (current != null) {
      ((Invokable) current).propagateLoopCountThroughoutLexicalScope(count);
    }
  }
}
