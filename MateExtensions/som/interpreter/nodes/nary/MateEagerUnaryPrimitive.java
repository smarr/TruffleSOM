package som.interpreter.nodes.nary;

import com.oracle.truffle.api.frame.VirtualFrame;

import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.MateAbstractReflectiveDispatch;
import som.interpreter.nodes.MateAbstractReflectiveDispatchNodeGen.MateDispatchMessageLookupNodeGen;
import som.interpreter.nodes.MateAbstractSemanticNodes.MateSemanticCheckNode;
import som.interpreter.nodes.MateAbstractSemanticNodesFactory.MateSemanticCheckNodeGen;
import som.vmobjects.SSymbol;


public class MateEagerUnaryPrimitive extends EagerUnaryPrimitiveNode implements MateMessage {
  @Child MateSemanticCheckNode                   semanticCheck;
  @Child MateAbstractReflectiveDispatch     reflectiveDispatch;
  
  public MateEagerUnaryPrimitive(SSymbol selector, ExpressionNode receiver,
      UnaryExpressionNode primitive) {
    super(selector, receiver, primitive);
    semanticCheck = MateSemanticCheckNodeGen.create(this.getSourceSection(), this.reflectiveOperation());
    reflectiveDispatch = MateDispatchMessageLookupNodeGen.create(this.getSourceSection());
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    Object rcvr = this.getReceiver().executeGeneric(frame);

    return executeEvaluated(frame, rcvr);
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
