package trufflesom.interpreter.nodes.nary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.vmobjects.SSymbol;


public abstract class TernaryMsgExprNode extends TernaryExpressionNode {

  public abstract SSymbol getSelector();

  @Fallback
  public final Object makeGenericSend(final VirtualFrame frame,
      final Object receiver, final Object arg1, final Object arg2) {
    CompilerDirectives.transferToInterpreterAndInvalidate();
    return makeGenericSend(getSelector()).doPreEvaluated(frame,
        new Object[] {receiver, arg1, arg2});
  }
}
