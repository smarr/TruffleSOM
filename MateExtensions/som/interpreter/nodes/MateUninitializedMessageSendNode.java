package som.interpreter.nodes;

import som.interpreter.nodes.MessageSendNode.UninitializedMessageSendNode;
import som.matenodes.MateAbstractReflectiveDispatch.MateAbstractStandardDispatch;
import som.matenodes.MateAbstractReflectiveDispatchFactory.MateDispatchMessageLookupNodeGen;
import som.matenodes.MateAbstractSemanticNodes.MateSemanticCheckNode;
import som.matenodes.MateBehavior;
import som.vm.MateSemanticsException;

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
}