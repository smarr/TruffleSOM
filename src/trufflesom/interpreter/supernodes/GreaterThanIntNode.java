package trufflesom.interpreter.supernodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.interpreter.bc.RespecializeException;
import trufflesom.interpreter.nodes.AbstractMessageSendNode;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.MessageSendNode;
import trufflesom.interpreter.nodes.bc.BytecodeLoopNode;
import trufflesom.interpreter.nodes.literals.IntegerLiteralNode;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.VmSettings;
import trufflesom.vmobjects.SSymbol;


public abstract class GreaterThanIntNode extends UnaryExpressionNode {

  private final long intValue;

  public GreaterThanIntNode(final long intValue) {
    this.intValue = intValue;
  }

  @Override
  public abstract ExpressionNode getReceiver();

  @Specialization
  public final boolean doLong(final long rcvr) {
    return rcvr > intValue;
  }

  @Specialization
  public final boolean doDouble(final double rcvr) {
    return rcvr > intValue;
  }

  @Fallback
  public final Object makeGenericSend(final VirtualFrame frame,
      final Object receiver) {
    CompilerDirectives.transferToInterpreterAndInvalidate();
    return makeGenericSend(SymbolTable.symbolFor(">")).doPreEvaluated(frame,
        new Object[] {receiver, intValue});
  }

  @Override
  protected AbstractMessageSendNode makeGenericSend(final SSymbol selector) {
    CompilerDirectives.transferToInterpreterAndInvalidate();
    AbstractMessageSendNode send =
        MessageSendNode.createGenericBinary(selector, getReceiver(),
            new IntegerLiteralNode(intValue), sourceCoord);

    if (VmSettings.UseAstInterp) {
      replace(send);
      send.notifyDispatchInserted();
      return send;
    }

    assert getParent() instanceof BytecodeLoopNode : "This node was expected to be a direct child of a `BytecodeLoopNode`.";
    throw new RespecializeException(send);
  }
}
