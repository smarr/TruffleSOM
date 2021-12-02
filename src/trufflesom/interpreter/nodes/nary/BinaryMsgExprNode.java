package trufflesom.interpreter.nodes.nary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.vmobjects.SSymbol;


public abstract class BinaryMsgExprNode extends BinaryExpressionNode {

  public abstract SSymbol getSelector();

  @Fallback
  public final Object makeGenericSend(final VirtualFrame frame,
      final Object receiver, final Object argument) {
    CompilerDirectives.transferToInterpreterAndInvalidate();
    return makeGenericSend(getSelector()).doPreEvaluated(frame,
        new Object[] {receiver, argument});
  }
}
