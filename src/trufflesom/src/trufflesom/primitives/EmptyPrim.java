package trufflesom.primitives;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SSymbol;


public final class EmptyPrim extends UnaryExpressionNode {
  @Child private ExpressionNode receiver;

  private final SSymbol signature;

  private EmptyPrim(final ExpressionNode receiver, final SSymbol signature) {
    this.receiver = receiver;
    this.signature = signature;
  }

  public EmptyPrim(final EmptyPrim node) {
    this(node.receiver, node.signature);
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    return executeEvaluated(frame, null);
  }

  @Override
  public Object executeEvaluated(final VirtualFrame frame, final Object rcvr) {
    CompilerDirectives.transferToInterpreter();
    Universe.errorExit(
        "Warning: undefined primitive called: " + signature + " at: " + getSourceSection());
    return null;
  }

  public static EmptyPrim create(final ExpressionNode receiver, final SSymbol signature) {
    return new EmptyPrim(receiver, signature);
  }

  @Override
  public ExpressionNode getReceiver() {
    return receiver;
  }
}
