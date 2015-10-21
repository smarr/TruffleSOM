package som.interpreter.nodes;

import som.interpreter.nodes.MessageSendNode.GenericMessageSendNode;
import som.interpreter.nodes.MessageSendNode.UninitializedMessageSendNode;
import som.interpreter.nodes.dispatch.UninitializedDispatchNode;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.EagerBinaryPrimitiveNode;
import som.interpreter.nodes.nary.MateEagerBinaryPrimitiveNode;
import som.matenodes.MateAbstractReflectiveDispatch.MateAbstractStandardDispatch;
import som.matenodes.MateAbstractReflectiveDispatchFactory.MateDispatchMessageLookupNodeGen;
import som.matenodes.MateAbstractSemanticNodes.MateSemanticCheckNode;
import som.matenodes.MateBehavior;
import som.vm.MateSemanticsException;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.frame.VirtualFrame;

public class MateUninitializedMessageSendNode extends
    UninitializedMessageSendNode implements MateBehavior {
  @Child MateSemanticCheckNode                   semanticCheck;
  @Child MateAbstractStandardDispatch     reflectiveDispatch;

  protected MateUninitializedMessageSendNode(UninitializedMessageSendNode somNode) {
    super(somNode.getSelector(), somNode.argumentNodes, somNode.getSourceSection());
    semanticCheck = MateSemanticCheckNode.createForFullCheck(this.getSourceSection(), this.reflectiveOperation());
    reflectiveDispatch = MateDispatchMessageLookupNodeGen.create(this.getSourceSection(), this.getSelector());
  }
  
  @Override
  public final Object executeGeneric(final VirtualFrame frame) {
    if (this.getSelector().toString().equals("#assert:description:")){
      int i = 1;
    }
    
    Object[] arguments = evaluateArguments(frame);
    try {
      return this.doMateSemantics(frame, arguments);
    } catch (MateSemanticsException e){
      return doPreEvaluated(frame, arguments);
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
  
  @Override
  protected GenericMessageSendNode makeGenericSend() {
    GenericMessageSendNode send = new MateGenericMessageSendNode(selector,
        argumentNodes,
        new UninitializedDispatchNode(selector),
        getSourceSection());
    return replace(send);
  }
  
  @Override
  protected EagerBinaryPrimitiveNode binaryPrimitiveFor(SSymbol selector, ExpressionNode receiver, ExpressionNode argument, BinaryExpressionNode primitive){
    return new MateEagerBinaryPrimitiveNode(selector, receiver, argument, primitive);
  }
}