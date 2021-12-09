package trufflesom.interpreter.nodes.nary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.interpreter.bc.RespecializeException;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.GenericMessageSendNode;
import trufflesom.interpreter.nodes.MessageSendNode;
import trufflesom.interpreter.nodes.bc.BytecodeLoopNode;
import trufflesom.vm.VmSettings;
import trufflesom.vmobjects.SSymbol;


@NodeChild(value = "receiver", type = ExpressionNode.class)
@NodeChild(value = "arg1", type = ExpressionNode.class)
@NodeChild(value = "arg2", type = ExpressionNode.class)
public abstract class TernaryExpressionNode extends EagerlySpecializableNode {

  public abstract Object executeEvaluated(
      VirtualFrame frame, Object receiver, Object arg1, Object arg2);

  public abstract ExpressionNode getReceiver();

  public abstract ExpressionNode getArg1();

  public abstract ExpressionNode getArg2();

  @Override
  public final Object doPreEvaluated(final VirtualFrame frame,
      final Object[] arguments) {
    return executeEvaluated(frame, arguments[0], arguments[1], arguments[2]);
  }

  protected GenericMessageSendNode makeGenericSend(final SSymbol selector) {
    CompilerDirectives.transferToInterpreterAndInvalidate();
    ExpressionNode[] children;
    if (VmSettings.UseAstInterp) {
      children = new ExpressionNode[] {getReceiver(), getArg1(), getArg2()};
    } else {
      children = null;
    }

    GenericMessageSendNode send =
        MessageSendNode.createGeneric(selector, children, null);

    if (VmSettings.UseAstInterp) {
      return replace(send);
    }

    assert getParent() instanceof BytecodeLoopNode : "This node was expected to be a direct child of a `BytecodeLoopNode`.";
    throw new RespecializeException(send);
  }
}
