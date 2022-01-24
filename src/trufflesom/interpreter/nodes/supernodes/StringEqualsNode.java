package trufflesom.interpreter.nodes.supernodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.interpreter.bc.RespecializeException;
import trufflesom.interpreter.nodes.AbstractMessageSendNode;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.MessageSendNode;
import trufflesom.interpreter.nodes.bc.BytecodeLoopNode;
import trufflesom.interpreter.nodes.literals.GenericLiteralNode;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.VmSettings;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SSymbol;


public abstract class StringEqualsNode extends UnaryExpressionNode {
  private final String value;

  protected static final Object nil = Nil.nilObject;

  protected StringEqualsNode(final String value) {
    this.value = value;
  }

  @Override
  public abstract ExpressionNode getReceiver();

  @Specialization
  public final boolean doString(final String rcvr) {
    return value.equals(rcvr);
  }

  @Specialization(guards = "rcvr == nil")
  public final boolean doNil(final Object rcvr) {
    return false;
  }

  @Fallback
  public final Object makeGenericSend(final VirtualFrame frame,
      final Object receiver) {
    CompilerDirectives.transferToInterpreterAndInvalidate();
    return makeGenericSend(SymbolTable.symbolFor("=")).doPreEvaluated(frame,
        new Object[] {receiver, value});
  }

  @Override
  protected AbstractMessageSendNode makeGenericSend(final SSymbol selector) {
    CompilerDirectives.transferToInterpreterAndInvalidate();
    AbstractMessageSendNode send =
        MessageSendNode.createGenericBinary(selector, getReceiver(),
            new GenericLiteralNode(value), sourceCoord);

    if (VmSettings.UseAstInterp) {
      replace(send);
      send.notifyDispatchInserted();
      return send;
    }

    assert getParent() instanceof BytecodeLoopNode : "This node was expected to be a direct child of a `BytecodeLoopNode`.";
    throw new RespecializeException(send);
  }
}
