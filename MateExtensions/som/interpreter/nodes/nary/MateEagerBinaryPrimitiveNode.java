package som.interpreter.nodes.nary;

import com.oracle.truffle.api.frame.VirtualFrame;

import som.interpreter.nodes.ExpressionNode;
import som.matenodes.MateAbstractReflectiveDispatch;
import som.matenodes.MateBehavior;
import som.matenodes.MateAbstractReflectiveDispatchNodeGen.MateDispatchMessageLookupNodeGen;
import som.matenodes.MateAbstractSemanticNodes.MateSemanticCheckNode;
import som.vm.MateSemanticsException;
import som.vmobjects.SSymbol;


public class MateEagerBinaryPrimitiveNode extends EagerBinaryPrimitiveNode implements MateBehavior {
  @Child MateSemanticCheckNode                   semanticCheck;
  @Child MateAbstractReflectiveDispatch     reflectiveDispatch;
  
  public MateEagerBinaryPrimitiveNode(SSymbol selector, ExpressionNode receiver, ExpressionNode argument,
      BinaryExpressionNode primitive) {
    super(selector, receiver, argument, primitive);
    semanticCheck = MateSemanticCheckNode.createForFullCheck(this.getSourceSection(), this.reflectiveOperation());
    reflectiveDispatch = MateDispatchMessageLookupNodeGen.create(this.getSourceSection());
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    Object rcvr = this.getReceiver().executeGeneric(frame);
    Object arg  = this.getArgument().executeGeneric(frame);
    try{
      return this.doMateSemantics(frame, new Object[] {rcvr, arg});
    } catch(MateSemanticsException e){
      return executeEvaluated(frame, rcvr, arg);
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
