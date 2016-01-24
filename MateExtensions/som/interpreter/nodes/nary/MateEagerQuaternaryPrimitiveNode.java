package som.interpreter.nodes.nary;

import som.interpreter.nodes.ExpressionNode;
import som.matenodes.MateBehavior;
import som.matenodes.MateAbstractReflectiveDispatch.MateAbstractStandardDispatch;
import som.matenodes.MateAbstractSemanticNodes.MateSemanticCheckNode;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.frame.VirtualFrame;

public class MateEagerQuaternaryPrimitiveNode extends EagerQuaternaryPrimitiveNode implements MateBehavior {
  @Child MateSemanticCheckNode                   semanticCheck;
  @Child MateAbstractStandardDispatch     reflectiveDispatch;
  
  public MateEagerQuaternaryPrimitiveNode(SSymbol selector, ExpressionNode receiver, ExpressionNode argument1, ExpressionNode argument2,
      ExpressionNode argument3, QuaternaryExpressionNode primitive) {
    super(selector, receiver, argument1, argument2, argument3, primitive);
    this.initializeMateSemantics(this.getSourceSection(), this.reflectiveOperation());
    this.initializeMateDispatchForMessages(this.getSourceSection(), this.getSelector());
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    Object rcvr = this.getReceiver().executeGeneric(frame);
    Object arg1 = this.getFirstArg().executeGeneric(frame);
    Object arg2 = this.getSecondArg().executeGeneric(frame);
    Object arg3 = this.getThirdArg().executeGeneric(frame);
    Object value = this.doMateSemantics(frame, new Object[] {rcvr, arg1, arg2, arg3});
    if (value == null){
     value = executeEvaluated(frame, rcvr, arg1, arg2, arg3);
    }
    return value;
  }

  @Override
  public MateSemanticCheckNode getMateNode() {
    return semanticCheck;
  }

  @Override
  public MateAbstractStandardDispatch getMateDispatch() {
    return reflectiveDispatch;
  }
  
  @Override
  public void setMateNode(MateSemanticCheckNode node) {
    semanticCheck = node;
  }

  @Override
  public void setMateDispatch(MateAbstractStandardDispatch node) {
    reflectiveDispatch = node;
  }
}
