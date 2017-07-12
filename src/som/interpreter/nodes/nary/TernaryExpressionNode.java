package som.interpreter.nodes.nary;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import som.interpreter.nodes.ExpressionNode;


@NodeChildren({
    @NodeChild(value = "receiver", type = ExpressionNode.class),
    @NodeChild(value = "firstArg", type = ExpressionNode.class),
    @NodeChild(value = "secondArg", type = ExpressionNode.class)})
public abstract class TernaryExpressionNode extends EagerlySpecializableNode {

  public TernaryExpressionNode(final SourceSection sourceSection) {
    super(sourceSection);
  }

  public TernaryExpressionNode() {
    this(null);
  }

  public abstract Object executeEvaluated(VirtualFrame frame, Object receiver, Object firstArg,
      Object secondArg);

  @Override
  public final Object doPreEvaluated(final VirtualFrame frame,
      final Object[] arguments) {
    return executeEvaluated(frame, arguments[0], arguments[1], arguments[2]);
  }
}
