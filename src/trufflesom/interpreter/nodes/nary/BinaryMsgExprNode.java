package trufflesom.interpreter.nodes.nary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.interpreter.SomLanguage;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.MessageSendNode;
import trufflesom.interpreter.nodes.MessageSendNode.GenericMessageSendNode;
import trufflesom.vmobjects.SSymbol;


public abstract class BinaryMsgExprNode extends BinaryExpressionNode {

  public abstract ExpressionNode getReceiver();

  public abstract ExpressionNode getArgument();

  public abstract SSymbol getSelector();

  @Fallback
  public final Object makeGenericSend(final VirtualFrame frame,
      final Object receiver, final Object argument) {
    return makeGenericSend().doPreEvaluated(frame,
        new Object[] {receiver, argument});
  }

  private GenericMessageSendNode makeGenericSend() {
    CompilerDirectives.transferToInterpreterAndInvalidate();
    GenericMessageSendNode node = MessageSendNode.createGeneric(getSelector(),
        new ExpressionNode[] {getReceiver(), getArgument()}, sourceSection,
        SomLanguage.getCurrentContext());
    return replace(node);
  }
}
