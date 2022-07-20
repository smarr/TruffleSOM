package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.bytecode.BytecodeLocal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.CountingConditionProfile;

import trufflesom.bdt.inlining.Inline;
import trufflesom.interpreter.Method.OpBuilder;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.NoPreEvalExprNode;
import trufflesom.vm.constants.Nil;


@Inline(selector = "ifNil:", inlineableArgIdx = 1)
public final class IfNilInlinedLiteralNode extends NoPreEvalExprNode {
  private final CountingConditionProfile condProf = CountingConditionProfile.create();

  @Child private ExpressionNode rcvr;
  @Child private ExpressionNode arg1;

  // In case we need to revert from this optimistic optimization, keep the
  // original nodes around
  @SuppressWarnings("unused") private final ExpressionNode originalArg1;

  public IfNilInlinedLiteralNode(final ExpressionNode rcvr, final ExpressionNode originalArg1,
      final ExpressionNode inlinedArg1) {
    this.rcvr = rcvr;
    this.originalArg1 = originalArg1;
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

  @Override
  public void constructOperation(final OpBuilder opBuilder) {
    BytecodeLocal receiverObj = opBuilder.dsl.createLocal();

    opBuilder.dsl.beginConditional();

    opBuilder.dsl.beginIsNil();
    opBuilder.dsl.beginBlock();
    opBuilder.dsl.beginStoreLocal(receiverObj);
    rcvr.accept(opBuilder);
    opBuilder.dsl.endStoreLocal();
    opBuilder.dsl.emitLoadLocal(receiverObj);
    opBuilder.dsl.endBlock();
    opBuilder.dsl.endIsNil();

    arg1.accept(opBuilder);
    opBuilder.dsl.emitLoadLocal(receiverObj);
    opBuilder.dsl.endConditional();
  }
}
