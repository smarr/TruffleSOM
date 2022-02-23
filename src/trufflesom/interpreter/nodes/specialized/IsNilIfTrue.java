package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;

import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.NoPreEvalExprNode;
import trufflesom.vm.constants.Nil;


public class IsNilIfTrue extends NoPreEvalExprNode {
  private final ConditionProfile condProf = ConditionProfile.createCountingProfile();

  @Child private ExpressionNode rcvr;
  @Child private ExpressionNode arg1;

  public IsNilIfTrue(final ExpressionNode rcvr, final ExpressionNode inlinedArg1) {
    this.rcvr = rcvr;
    this.arg1 = inlinedArg1;
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    Object r = rcvr.executeGeneric(frame);
    if (condProf.profile(r == Nil.nilObject)) {
      return arg1.executeGeneric(frame);
    }
    return r;
  }

}
