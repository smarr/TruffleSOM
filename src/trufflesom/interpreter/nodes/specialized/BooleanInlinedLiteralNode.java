package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import bdt.inlining.Inline;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.NoPreEvalExprNode;


public abstract class BooleanInlinedLiteralNode extends NoPreEvalExprNode {

  @Child protected ExpressionNode receiverNode;
  @Child protected ExpressionNode argumentNode;

  // In case we need to revert from this optimistic optimization, keep the
  // original nodes around
  @SuppressWarnings("unused") private final ExpressionNode argumentActualNode;

  public BooleanInlinedLiteralNode(final ExpressionNode receiverNode,
      final ExpressionNode inlinedArgumentNode, final ExpressionNode originalArgumentNode) {
    this.receiverNode = receiverNode;
    this.argumentNode = inlinedArgumentNode;
    this.argumentActualNode = originalArgumentNode;
  }

  protected final boolean evaluateReceiver(final VirtualFrame frame) {
    try {
      return receiverNode.executeBoolean(frame);
    } catch (UnexpectedResultException e) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      // TODO: should rewrite to a node that does a proper message send...
      throw new UnsupportedSpecializationException(this,
          new Node[] {receiverNode}, e.getResult());
    }
  }

  protected final boolean evaluateArgument(final VirtualFrame frame) {
    try {
      return argumentNode.executeBoolean(frame);
    } catch (UnexpectedResultException e) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      // TODO: should rewrite to a node that does a proper message send...
      throw new UnsupportedSpecializationException(this,
          new Node[] {argumentNode}, e.getResult());
    }
  }

  @Inline(selector = "and:", inlineableArgIdx = 1)
  @Inline(selector = "&&", inlineableArgIdx = 1)
  public static final class AndInlinedLiteralNode extends BooleanInlinedLiteralNode {

    public AndInlinedLiteralNode(final ExpressionNode receiverNode,
        final ExpressionNode originalArgumentNode, final ExpressionNode inlinedArgumentNode) {
      super(receiverNode, inlinedArgumentNode, originalArgumentNode);
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

  @Inline(selector = "or:", inlineableArgIdx = 1)
  @Inline(selector = "||", inlineableArgIdx = 1)
  public static final class OrInlinedLiteralNode extends BooleanInlinedLiteralNode {

    public OrInlinedLiteralNode(final ExpressionNode receiverNode,
        final ExpressionNode originalArgumentNode, final ExpressionNode inlinedArgumentNode) {
      super(receiverNode, inlinedArgumentNode, originalArgumentNode);
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
