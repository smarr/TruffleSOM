package trufflesom.interpreter.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;


public abstract class NoPreEvalExprNode extends ExpressionNode {

  @Override
  public abstract Object executeGeneric(VirtualFrame frame);

  @Override
  public final Object doPreEvaluated(final VirtualFrame frame, final Object[] args) {
    CompilerDirectives.transferToInterpreter();
    throw new UnsupportedOperationException();
  }
}
