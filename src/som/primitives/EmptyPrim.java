package som.primitives;

import com.oracle.truffle.api.frame.VirtualFrame;

import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.Universe;


public final class EmptyPrim extends UnaryExpressionNode {
  @Child private ExpressionNode receiver;

  private EmptyPrim(final ExpressionNode receiver) {
    this.receiver = receiver;
  }

  public EmptyPrim(final EmptyPrim node) {
    this(node.receiver);
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    return executeEvaluated(frame, null);
  }

  @Override
  public Object executeEvaluated(final VirtualFrame frame, final Object receiver) {
    Universe.println("Warning: undefined primitive called");
    return null;
  }

  public static EmptyPrim create(final ExpressionNode receiver) {
    return new EmptyPrim(receiver);
  }
}
