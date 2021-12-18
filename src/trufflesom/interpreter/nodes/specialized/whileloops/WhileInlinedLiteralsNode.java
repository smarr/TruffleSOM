package trufflesom.interpreter.nodes.specialized.whileloops;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import bd.inlining.Inline;
import bd.inlining.Inline.False;
import bd.inlining.Inline.True;
import trufflesom.interpreter.Invokable;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.NoPreEvalExprNode;
import trufflesom.vm.constants.Nil;


@Inline(selector = "whileTrue:", inlineableArgIdx = {0, 1}, additionalArgs = True.class)
@Inline(selector = "whileFalse:", inlineableArgIdx = {0, 1}, additionalArgs = False.class)
public final class WhileInlinedLiteralsNode extends NoPreEvalExprNode {

  @Child private ExpressionNode conditionNode;
  @Child private ExpressionNode bodyNode;

  private final boolean expectedBool;

  @SuppressWarnings("unused") private final ExpressionNode conditionActualNode;
  @SuppressWarnings("unused") private final ExpressionNode bodyActualNode;

  public WhileInlinedLiteralsNode(final ExpressionNode originalConditionNode,
      final ExpressionNode originalBodyNode, final ExpressionNode inlinedConditionNode,
      final ExpressionNode inlinedBodyNode, final boolean expectedBool) {
    this.conditionNode = inlinedConditionNode;
    this.bodyNode = inlinedBodyNode;
    this.expectedBool = expectedBool;
    this.conditionActualNode = originalConditionNode;
    this.bodyActualNode = originalBodyNode;
  }

  private boolean evaluateCondition(final VirtualFrame frame) {
    try {
      return conditionNode.executeBoolean(frame);
    } catch (UnexpectedResultException e) {
      // TODO: should rewrite to a node that does a proper message send...
      throw new UnsupportedSpecializationException(this,
          new Node[] {conditionNode}, e.getResult());
    }
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    long iterationCount = 0;

    // TODO: this is a simplification, we don't cover the case receiver isn't a boolean
    boolean loopConditionResult = evaluateCondition(frame);

    try {
      while (loopConditionResult == expectedBool) {
        bodyNode.executeGeneric(frame);
        loopConditionResult = evaluateCondition(frame);

        if (CompilerDirectives.inInterpreter()) {
          iterationCount++;
        }
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount(iterationCount);
      }
    }
    return Nil.nilObject;
  }

  protected void reportLoopCount(final long count) {
    CompilerAsserts.neverPartOfCompilation("reportLoopCount");
    Node current = getParent();
    while (current != null && !(current instanceof RootNode)) {
      current = current.getParent();
    }
    if (current != null) {
      ((Invokable) current).propagateLoopCountThroughoutLexicalScope(count);
    }
  }
}
