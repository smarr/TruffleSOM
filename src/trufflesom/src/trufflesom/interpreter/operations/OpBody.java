package trufflesom.interpreter.operations;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.interpreter.nodes.ExpressionNode;


// TODO: Try to get rid of this, the opNode is already a root node
public class OpBody extends ExpressionNode {
  private final SomOperations opNode;

  public OpBody(final SomOperations opNode) {
    this.opNode = opNode;
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    return opNode.execute(frame);
  }

  @Override
  public Object doPreEvaluated(final VirtualFrame frame, final Object[] args) {
    CompilerDirectives.transferToInterpreterAndInvalidate();
    throw new UnsupportedOperationException();
  }
}
