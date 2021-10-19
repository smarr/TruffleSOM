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


public final class EagerUnaryPrimitiveNode extends EagerPrimitive {

  @Child private ExpressionNode      receiver;
  @Child private UnaryExpressionNode primitive;

  private final SSymbol  selector;
  private final Universe universe;

  public EagerUnaryPrimitiveNode(final SSymbol selector, final ExpressionNode receiver,
      final UnaryExpressionNode primitive, final Universe universe) {
    this.receiver = insert(receiver);
    this.primitive = insert(primitive);
    this.selector = selector;
    this.universe = universe;
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    Object rcvr = receiver.executeGeneric(frame);

    return executeEvaluated(frame, rcvr);
  }

  public Object executeEvaluated(final VirtualFrame frame,
      final Object receiver) {
    try {
      return primitive.executeEvaluated(frame, receiver);
    } catch (UnsupportedSpecializationException e) {
      TruffleCompiler.transferToInterpreterAndInvalidate(
          "Eager Primitive with unsupported specialization.");
      return makeGenericSend().doPreUnary(frame, receiver);
    }
  }

  @Override
  public Object doPreEvaluated(final VirtualFrame frame, final Object[] arguments) {
    return executeEvaluated(frame, arguments[0]);
  }

  @Override
  public Object doPreUnary(final VirtualFrame frame, final Object rcvr) {
    return executeEvaluated(frame, rcvr);
  }

  @Override
  public Object doPreBinary(final VirtualFrame frame, final Object rcvr, final Object arg) {
    CompilerDirectives.transferToInterpreter();
    throw new UnsupportedOperationException();
  }

  @Override
  public Object doPreTernary(final VirtualFrame frame, final Object rcvr, final Object arg1,
      final Object arg2) {
    CompilerDirectives.transferToInterpreter();
    throw new UnsupportedOperationException();
  }

  @Override
  public Object doPreQuat(final VirtualFrame frame, final Object rcvr, final Object arg1,
      final Object arg2, final Object arg3) {
    CompilerDirectives.transferToInterpreter();
    throw new UnsupportedOperationException();
  }

  private AbstractMessageSendNode makeGenericSend() {
    AbstractMessageSendNode node = MessageSendNode.createGeneric(selector,
        new ExpressionNode[] {receiver}, sourceSection, universe);
    return replace(node);
  }

  @Override
  public SSymbol getInvocationIdentifier() {
    return selector;
  }
}
