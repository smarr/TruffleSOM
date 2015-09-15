package som.interpreter.nodes;

import som.interpreter.nodes.MateDispatch.MateDispatchFieldAccess;
import som.interpreter.nodes.MateDispatch.MateDispatchMessageSend;
import som.interpreter.nodes.MessageSendNode.AbstractMessageSendNode;
import som.interpreter.objectstorage.FieldAccessorNode;
import som.vm.MateUniverse;
import som.vmobjects.SMateEnvironment;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public abstract class MateExpressionNode extends ExpressionNode implements MateNode {
  @Child protected MateDispatch reflectiveDispatch;
  protected Object[] arguments;
  protected SMateEnvironment environment;
  
  public MateExpressionNode(final MateDispatch node, Node wrappedNode) {
    super(node.getSourceSection());
    reflectiveDispatch = node;
  }
  
  public static MateExpressionNode createForMessageSend(AbstractMessageSendNode node){
    return MateExpressionNodeGen.create(MateDispatchMessageSend.create(node), node);
  }
  
  public static MateExpressionNode createForFieldAccess(FieldAccessorNode node){
    return MateExpressionNodeGen.create(MateDispatchFieldAccess.create(node), node);
  }
  
  public static MateExpressionNode createForGenericExpression(Node node){
    return MateExpressionNodeGen.create(MateDispatch.create(node), node);
  }
  
  @Specialization(guards="hasReflectiveBehavior(frame)")
  public Object doMetaLevel(VirtualFrame frame){
    return this.metaExecution(frame);
  }
  
  @Specialization(guards="!hasReflectiveBehavior(frame)")
  public Object doBaseLevel(VirtualFrame frame) {
    //return ((ExpressionNode)this.getReflectiveDispatch().getBaseLevel()).executeGeneric(frame);
    return baseExecution(frame);
  }
  
  public void setEnvironment(SMateEnvironment env){
    environment = env;
  }
  
  public SMateEnvironment getEnvironment(){
    return environment;
  }
  
  public MateDispatch getReflectiveDispatch(){
    return this.reflectiveDispatch;
  }
  
  public Object[] getArguments(){
    return arguments;
  }
  
  public void setArguments(Object[] args){
    arguments = args;
  }
  
  public Object[] evaluateArguments(final VirtualFrame frame) {
    arguments = this.reflectiveDispatch.evaluateArguments(frame);
    return arguments;
  }  
}