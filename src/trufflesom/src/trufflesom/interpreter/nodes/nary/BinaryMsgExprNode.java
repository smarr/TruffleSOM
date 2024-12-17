package trufflesom.interpreter.nodes.nary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.frame.VirtualFrame;

import com.oracle.truffle.api.nodes.Node;
import trufflesom.vmobjects.SSymbol;


public abstract class BinaryMsgExprNode extends BinaryExpressionNode {

  public abstract SSymbol getSelector();

  @Fallback
  public static final Object makeGenericSend(final VirtualFrame frame,
      final Object receiver, final Object argument, @Bind Node s) {
    CompilerDirectives.transferToInterpreterAndInvalidate();
    BinaryMsgExprNode self = (BinaryMsgExprNode) s;
    return self.makeGenericSend(self.getSelector()).doPreEvaluated(frame,
        new Object[] {receiver, argument});
  }
}
