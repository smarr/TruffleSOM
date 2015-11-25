package som.interpreter.nodes;

import som.interpreter.SArguments;
import som.matenodes.MateAbstractReflectiveDispatch.MateActivationDispatch;
import som.matenodes.MateAbstractReflectiveDispatchFactory.MateActivationDispatchNodeGen;
import som.matenodes.MateAbstractSemanticNodes.MateSemanticCheckNode;
import som.vm.MateSemanticsException;
import som.vm.constants.ExecutionLevel;
import som.vm.constants.ReflectiveOp;
import som.vmobjects.SInvokable.SMethod;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;


public class MateMethodActivationNode extends Node {
  @Child MateSemanticCheckNode  semanticCheck;
  @Child MateActivationDispatch reflectiveDispatch;
  
  public MateMethodActivationNode(){
    semanticCheck = MateSemanticCheckNode.createForFullCheck(this.getSourceSection(), ReflectiveOp.Activation);
    reflectiveDispatch = MateActivationDispatchNodeGen.create(this.getSourceSection());
  }
  
  public Object doActivation(final VirtualFrame frame, SMethod method, Object[] arguments) {
    try{
      return this.getMateDispatch().executeDispatch(frame, this.getMateNode().execute(frame, arguments), method, arguments);
    } catch(MateSemanticsException e){
      return method.getCallTarget().call(SArguments.createSArguments(SArguments.getEnvironment(frame), ExecutionLevel.Base, arguments));
    }
  }

  public MateSemanticCheckNode getMateNode() {
    return semanticCheck;
  }

  public MateActivationDispatch getMateDispatch() {
    return reflectiveDispatch;
  }
}
