package som.interpreter.nodes.nary;

import som.interpreter.nodes.ExpressionNode;
import som.matenodes.MateAbstractReflectiveDispatch.MateAbstractStandardDispatch;
import som.matenodes.MateAbstractReflectiveDispatchFactory.MateDispatchMessageLookupNodeGen;
import som.matenodes.MateAbstractSemanticNodes.MateSemanticCheckNode;
import som.matenodes.MateBehavior;
import som.vm.MateSemanticsException;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.frame.VirtualFrame;


public class MateEagerBinaryPrimitiveNode extends EagerBinaryPrimitiveNode implements MateBehavior {
  @Child MateSemanticCheckNode                   semanticCheck;
  @Child MateAbstractStandardDispatch     reflectiveDispatch;
  
  public MateEagerBinaryPrimitiveNode(SSymbol selector, ExpressionNode receiver, ExpressionNode argument,
      BinaryExpressionNode primitive) {
    super(selector, receiver, argument, primitive);
    semanticCheck = MateSemanticCheckNode.createForFullCheck(this.getSourceSection(), this.reflectiveOperation());
    reflectiveDispatch = MateDispatchMessageLookupNodeGen.create(this.getSourceSection(), this.getSelector());
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
  public MateAbstractStandardDispatch getMateDispatch() {
    return reflectiveDispatch;
  }
}
