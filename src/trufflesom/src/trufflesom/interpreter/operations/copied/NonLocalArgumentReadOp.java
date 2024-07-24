package trufflesom.interpreter.operations.copied;

import com.oracle.truffle.api.bytecode.ConstantOperand;
import com.oracle.truffle.api.bytecode.OperationProxy.Proxyable;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import trufflesom.interpreter.nodes.ContextualNode;
import trufflesom.interpreter.nodes.ExpressionNode;


@Proxyable
@NodeChild(value = "argIndex", type = ExpressionNode.class)
@NodeChild(value = "contextLevel", type = ExpressionNode.class)
@ConstantOperand(type = int.class)
@ConstantOperand(type = int.class)
public abstract class NonLocalArgumentReadOp extends Node {
  public abstract Object executeGeneric(VirtualFrame frame);

  @Specialization
  public static final Object read(final VirtualFrame frame, final int argIndex,
      final int contextLevel) {
    return ContextualNode.determineContext(frame, contextLevel)
                         .getArguments()[argIndex];
  }
}
