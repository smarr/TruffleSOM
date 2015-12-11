package som.interpreter.nodes;

import som.interpreter.SArguments;
import som.interpreter.nodes.dispatch.AbstractMethodDispatchNode;
import som.interpreter.nodes.dispatch.GenericMethodDispatchNode;
import som.interpreter.nodes.dispatch.UninitializedMethodDispatchNode;
import som.matenodes.MateAbstractReflectiveDispatch.MateActivationDispatch;
import som.matenodes.MateAbstractReflectiveDispatchFactory.MateActivationDispatchNodeGen;
import som.matenodes.MateAbstractSemanticNodes.MateSemanticCheckNode;
import som.vm.MateSemanticsException;
import som.vm.constants.ExecutionLevel;
import som.vm.constants.ReflectiveOp;
import som.vmobjects.SInvokable.SMethod;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;


public class MateMethodActivationNode extends Node {
  @Child MateSemanticCheckNode  semanticCheck;
  @Child MateActivationDispatch reflectiveDispatch;
  @Child AbstractMethodDispatchNode methodDispatch;
  
  public MateMethodActivationNode(){
    semanticCheck = MateSemanticCheckNode.createForFullCheck(this.getSourceSection(), ReflectiveOp.Activation);
    reflectiveDispatch = MateActivationDispatchNodeGen.create(this.getSourceSection());
    methodDispatch = new UninitializedMethodDispatchNode();
  }
  
  public Object doActivation(final VirtualFrame frame, SMethod method, Object[] arguments) {
    try{
      return this.getMateDispatch().executeDispatch(frame, this.getMateNode().execute(frame, arguments), method, arguments);
    } catch(MateSemanticsException e){
      return methodDispatch.executeDispatch(frame, SArguments.getEnvironment(frame), ExecutionLevel.Base, method, arguments);
    }
  }

  public MateSemanticCheckNode getMateNode() {
    return semanticCheck;
  }

  public MateActivationDispatch getMateDispatch() {
    return reflectiveDispatch;
  }
  
  public AbstractMethodDispatchNode getDispatchListHead() {
    return methodDispatch;
  }

  public void adoptNewDispatchListHead(final AbstractMethodDispatchNode newHead) {
    CompilerAsserts.neverPartOfCompilation();
    methodDispatch = insert(newHead);
  }

  public void replaceDispatchListHead(
      final GenericMethodDispatchNode replacement) {
    CompilerAsserts.neverPartOfCompilation();
    methodDispatch.replace(replacement);
  }

}
