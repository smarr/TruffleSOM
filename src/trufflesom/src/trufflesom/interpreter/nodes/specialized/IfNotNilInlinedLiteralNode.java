package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.CountingConditionProfile;

import trufflesom.bdt.inlining.Inline;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.NoPreEvalExprNode;
import trufflesom.vm.constants.Nil;


@Inline(selector = "ifNotNil:", inlineableArgIdx = 1)
public final class IfNotNilInlinedLiteralNode extends NoPreEvalExprNode {
  private final CountingConditionProfile condProf = CountingConditionProfile.create();

  @Child private ExpressionNode rcvr;
  @Child private ExpressionNode arg1;

  // In case we need to revert from this optimistic optimization, keep the
  // original nodes around
  @SuppressWarnings("unused") private final ExpressionNode originalArg1;

  public IfNotNilInlinedLiteralNode(final ExpressionNode rcvr,
      final ExpressionNode originalArg1,
      final ExpressionNode inlinedArg1) {
    this.rcvr = rcvr;
    this.originalArg1 = originalArg1;
    this.arg1 = inlinedArg1;
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    Object r = rcvr.executeGeneric(frame);

    if (condProf.profile(r != Nil.nilObject)) {
      return arg1.executeGeneric(frame);
    }
    return r;
  }

}
