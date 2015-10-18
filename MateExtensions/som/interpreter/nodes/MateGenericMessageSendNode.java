package som.interpreter.nodes;

import som.interpreter.nodes.MessageSendNode.GenericMessageSendNode;
import som.matenodes.MateAbstractReflectiveDispatch.MateAbstractStandardDispatch;
import som.matenodes.MateAbstractReflectiveDispatchFactory.MateDispatchMessageLookupNodeGen;
import som.matenodes.MateAbstractSemanticNodes.MateSemanticCheckNode;
import som.matenodes.MateBehavior;
import som.vm.MateSemanticsException;

import com.oracle.truffle.api.frame.VirtualFrame;


public class MateGenericMessageSendNode extends GenericMessageSendNode implements MateBehavior {
  @Child MateSemanticCheckNode                   semanticCheck;
  @Child MateAbstractStandardDispatch     reflectiveDispatch;

  protected MateGenericMessageSendNode(GenericMessageSendNode somNode) {
    super(somNode.getSelector(), somNode.argumentNodes, somNode.getDispatchListHead(), somNode.getSourceSection());
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
