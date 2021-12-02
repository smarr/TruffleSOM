package trufflesom.interpreter.nodes.nary;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.vmobjects.SSymbol;


public abstract class UnaryMsgExprNode extends UnaryExpressionNode {
  public abstract SSymbol getSelector();

  @Fallback
  public final Object makeGenericSend(final VirtualFrame frame, final Object receiver) {
    return makeGenericSend(getSelector()).doPreEvaluated(frame, new Object[] {receiver});
  }
}
