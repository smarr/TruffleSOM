package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.CountingConditionProfile;
import trufflesom.bdt.inlining.Inline;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.NoPreEvalExprNode;
import trufflesom.vm.constants.Nil;


@Inline(selector = "ifNil:ifNotNil:", inlineableArgIdx = {1, 2})
public final class IfNilNotNilInlinedLiteralNode extends NoPreEvalExprNode {
  private final CountingConditionProfile condProf = CountingConditionProfile.create();

  @Child private ExpressionNode rcvr;
  @Child private ExpressionNode arg1;
  @Child private ExpressionNode arg2;

  // In case we need to revert from this optimistic optimization, keep the
  // original nodes around
  @SuppressWarnings("unused") private final ExpressionNode originalArg1;
  @SuppressWarnings("unused") private final ExpressionNode originalArg2;

  public IfNilNotNilInlinedLiteralNode(final ExpressionNode rcvr,
      final ExpressionNode originalArg1,
      final ExpressionNode originalArg2,
      final ExpressionNode inlinedArg1,
      final ExpressionNode inlinedArg2) {
    this.rcvr = rcvr;
    this.originalArg1 = originalArg1;
    this.originalArg2 = originalArg2;
    this.arg1 = inlinedArg1;
    this.arg2 = inlinedArg2;
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    Object r = rcvr.executeGeneric(frame);

    if (condProf.profile(r == Nil.nilObject)) {
      return arg1.executeGeneric(frame);
    }
    return arg2.executeGeneric(frame);
  }
}
