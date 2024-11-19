package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.CountingConditionProfile;

import trufflesom.bdt.inlining.Inline;
import trufflesom.bdt.inlining.Inline.False;
import trufflesom.bdt.inlining.Inline.True;
import trufflesom.interpreter.Method.OpBuilder;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.NoPreEvalExprNode;
import trufflesom.vm.constants.Nil;


@Inline(selector = "ifTrue:", inlineableArgIdx = 1, additionalArgs = True.class)
@Inline(selector = "ifFalse:", inlineableArgIdx = 1, additionalArgs = False.class)
public final class IfInlinedLiteralNode extends NoPreEvalExprNode {
  private final CountingConditionProfile condProf = CountingConditionProfile.create();

  @Child private ExpressionNode conditionNode;
  @Child private ExpressionNode bodyNode;

  private final boolean expectedBool;

  // In case we need to revert from this optimistic optimization, keep the
  // original nodes around
  @SuppressWarnings("unused") private final ExpressionNode bodyActualNode;

  public IfInlinedLiteralNode(final ExpressionNode conditionNode,
      final ExpressionNode originalBodyNode, final ExpressionNode inlinedBodyNode,
      final boolean expectedBool) {
    this.conditionNode = conditionNode;
    this.expectedBool = expectedBool;
    this.bodyNode = inlinedBodyNode;
    this.bodyActualNode = originalBodyNode;
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    boolean cond;
    try {
      cond = condProf.profile(conditionNode.executeBoolean(frame));
    } catch (UnexpectedResultException e) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      // TODO: should rewrite to a node that does a proper message send...
      throw new UnsupportedSpecializationException(this,
          new Node[] {conditionNode}, e.getResult());
    }

    if (cond == expectedBool) {
      return bodyNode.executeGeneric(frame);
    } else {
      return Nil.nilObject;
    }
  }

  @Override
  public void constructOperation(final OpBuilder opBuilder) {
    opBuilder.dsl.beginConditional();
    conditionNode.accept(opBuilder);

    if (expectedBool) {
      // then branch
      bodyNode.accept(opBuilder);

      // else branch
      opBuilder.dsl.emitLoadConstant(Nil.nilObject);
    } else {
      // then branch
      opBuilder.dsl.emitLoadConstant(Nil.nilObject);

      // else branch
      bodyNode.accept(opBuilder);
    }

    opBuilder.dsl.endConditional();
  }
}
