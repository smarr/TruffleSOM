package trufflesom.interpreter.nodes.nary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.interpreter.TruffleCompiler;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.MessageSendNode;
import trufflesom.interpreter.nodes.MessageSendNode.AbstractMessageSendNode;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SSymbol;


public final class EagerTernaryPrimitiveNode extends EagerPrimitive {

  @Child private ExpressionNode        receiver;
  @Child private ExpressionNode        argument1;
  @Child private ExpressionNode        argument2;
  @Child private TernaryExpressionNode primitive;

  private final SSymbol  selector;
  private final Universe universe;

  public EagerTernaryPrimitiveNode(final SSymbol selector, final ExpressionNode receiver,
      final ExpressionNode argument1, final ExpressionNode argument2,
      final TernaryExpressionNode primitive, final Universe universe) {
    this.receiver = insert(receiver);
    this.argument1 = insert(argument1);
    this.argument2 = insert(argument2);
    this.primitive = insert(primitive);
    this.selector = selector;
    this.universe = universe;
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    Object rcvr = receiver.executeGeneric(frame);
    Object arg1 = argument1.executeGeneric(frame);
    Object arg2 = argument2.executeGeneric(frame);

    return executeEvaluated(frame, rcvr, arg1, arg2);
  }

  public Object executeEvaluated(final VirtualFrame frame,
      final Object receiver, final Object argument1, final Object argument2) {
    try {
      return primitive.executeEvaluated(frame, receiver, argument1, argument2);
    } catch (UnsupportedSpecializationException e) {
      TruffleCompiler.transferToInterpreterAndInvalidate(
          "Eager Primitive with unsupported specialization.");
      return makeGenericSend().doPreTernary(frame, receiver, argument1, argument2);
    }
  }

  @Override
  public Object doPreEvaluated(final VirtualFrame frame,
      final Object[] arguments) {
    return executeEvaluated(frame, arguments[0], arguments[1], arguments[2]);
  }

  @Override
  public Object doPreUnary(final VirtualFrame frame, final Object rcvr) {
    CompilerDirectives.transferToInterpreter();
    throw new UnsupportedOperationException();
  }

  @Override
  public Object doPreBinary(final VirtualFrame frame, final Object rcvr, final Object arg) {
    CompilerDirectives.transferToInterpreter();
    throw new UnsupportedOperationException();
  }

  @Override
  public Object doPreTernary(final VirtualFrame frame, final Object rcvr, final Object arg1,
      final Object arg2) {
    return executeEvaluated(frame, rcvr, arg1, arg2);
  }

  @Override
  public Object doPreQuat(final VirtualFrame frame, final Object rcvr, final Object arg1,
      final Object arg2, final Object arg3) {
    CompilerDirectives.transferToInterpreter();
    throw new UnsupportedOperationException();
  }

  private AbstractMessageSendNode makeGenericSend() {
    AbstractMessageSendNode node = MessageSendNode.createGeneric(selector,
        new ExpressionNode[] {receiver, argument1, argument2}, sourceSection, universe);
    return replace(node);
  }

  @Override
  public SSymbol getInvocationIdentifier() {
    return selector;
  }
}
