package som.interpreter.nodes.nary;

import com.oracle.truffle.api.frame.VirtualFrame;

import som.interpreter.nodes.ExpressionNode;
import som.matenodes.MateAbstractReflectiveDispatch;
import som.matenodes.MateBehavior;
import som.matenodes.MateAbstractReflectiveDispatchNodeGen.MateDispatchMessageLookupNodeGen;
import som.matenodes.MateAbstractSemanticNodes.MateSemanticCheckNode;
import som.vm.MateSemanticsException;
import som.vmobjects.SSymbol;


public class MateEagerTernaryPrimitiveNode extends EagerTernaryPrimitiveNode implements MateBehavior {
  @Child MateSemanticCheckNode                   semanticCheck;
  @Child MateAbstractReflectiveDispatch     reflectiveDispatch;
  
  public MateEagerTernaryPrimitiveNode(SSymbol selector, ExpressionNode receiver, ExpressionNode argument1, ExpressionNode argument2,
      TernaryExpressionNode primitive) {
    super(selector, receiver, argument1, argument2, primitive);
    semanticCheck = MateSemanticCheckNode.createForFullCheck(this.getSourceSection(), this.reflectiveOperation());
    reflectiveDispatch = MateDispatchMessageLookupNodeGen.create(this.getSourceSection());
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    Object rcvr = this.getReceiver().executeGeneric(frame);
    Object arg1 = this.getFirstArg().executeGeneric(frame);
    Object arg2 = this.getSecondArg().executeGeneric(frame);
    try{
      return this.doMateSemantics(frame, new Object[] {rcvr, arg1, arg2});
    } catch(MateSemanticsException e){
      return executeEvaluated(frame, rcvr, arg1, arg2);
    }
  }

  @Override
  public MateSemanticCheckNode getMateNode() {
    return semanticCheck;
  }

  @Override
  public MateAbstractReflectiveDispatch getMateDispatch() {
    return reflectiveDispatch;
  }

}
