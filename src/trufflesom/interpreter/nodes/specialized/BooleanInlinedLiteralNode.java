package som.interpreter.nodes.specialized;

import som.interpreter.nodes.ExpressionNode;

import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;


public abstract class BooleanInlinedLiteralNode extends ExpressionNode {

  @Child protected ExpressionNode receiverNode;
  @Child protected ExpressionNode argumentNode;

  // In case we need to revert from this optimistic optimization, keep the
  // original nodes around
  private final ExpressionNode argumentAcutalNode;

  public BooleanInlinedLiteralNode(
      final ExpressionNode receiverNode,
      final ExpressionNode inlinedArgumentNode,
      final ExpressionNode originalArgumentNode,
      final SourceSection sourceSection) {
    super(sourceSection);
    this.receiverNode = receiverNode;
    this.argumentNode = inlinedArgumentNode;
    this.argumentAcutalNode = originalArgumentNode;
  }

  protected final boolean evaluateReceiver(final VirtualFrame frame) {
    try {
      return receiverNode.executeBoolean(frame);
    } catch (UnexpectedResultException e) {
      // TODO: should rewrite to a node that does a proper message send...
      throw new UnsupportedSpecializationException(this,
          new Node[] {receiverNode}, e.getResult());
    }
  }

  protected final boolean evaluateArgument(final VirtualFrame frame) {
    try {
      return argumentNode.executeBoolean(frame);
    } catch (UnexpectedResultException e) {
      // TODO: should rewrite to a node that does a proper message send...
      throw new UnsupportedSpecializationException(this,
          new Node[] {argumentNode}, e.getResult());
    }
  }

  public static final class AndInlinedLiteralNode extends BooleanInlinedLiteralNode {

    public AndInlinedLiteralNode(
        final ExpressionNode receiverNode,
        final ExpressionNode inlinedArgumentNode,
        final ExpressionNode originalArgumentNode,
        final SourceSection sourceSection) {
      super(receiverNode, inlinedArgumentNode, originalArgumentNode,
          sourceSection);
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      return executeBoolean(frame);
    }

    @Override
    public boolean executeBoolean(final VirtualFrame frame) {
      if (evaluateReceiver(frame)) {
        return evaluateArgument(frame);
      } else {
        return false;
      }
    }
  }

  public static final class OrInlinedLiteralNode extends BooleanInlinedLiteralNode {

    public OrInlinedLiteralNode(
        final ExpressionNode receiverNode,
        final ExpressionNode inlinedArgumentNode,
        final ExpressionNode originalArgumentNode,
        final SourceSection sourceSection) {
      super(receiverNode, inlinedArgumentNode, originalArgumentNode,
          sourceSection);
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      return executeBoolean(frame);
    }

    @Override
    public boolean executeBoolean(final VirtualFrame frame) {
      if (evaluateReceiver(frame)) {
        return true;
      } else {
        return evaluateArgument(frame);
      }
    }
  }

}
