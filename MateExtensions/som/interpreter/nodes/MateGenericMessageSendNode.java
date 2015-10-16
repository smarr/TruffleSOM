package som.interpreter.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import som.interpreter.nodes.MessageSendNode.GenericMessageSendNode;
import som.matenodes.MateAbstractReflectiveDispatch;
import som.matenodes.MateBehavior;
import som.matenodes.MateAbstractReflectiveDispatchNodeGen.MateDispatchFieldAccessNodeGen;
import som.matenodes.MateAbstractSemanticNodes.MateSemanticCheckNode;
import som.vm.MateSemanticsException;


public class MateGenericMessageSendNode extends GenericMessageSendNode implements MateBehavior {
  @Child MateSemanticCheckNode                   semanticCheck;
  @Child MateAbstractReflectiveDispatch     reflectiveDispatch;

  protected MateGenericMessageSendNode(GenericMessageSendNode somNode) {
    super(somNode.getSelector(), somNode.argumentNodes, somNode.getDispatchListHead(), somNode.getSourceSection());
    semanticCheck = MateSemanticCheckNode.createForFullCheck(this.getSourceSection(), this.reflectiveOperation());
    reflectiveDispatch = MateDispatchFieldAccessNodeGen.create(this.getSourceSection());
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
  public MateAbstractReflectiveDispatch getMateDispatch() {
    return reflectiveDispatch;
  }

}
