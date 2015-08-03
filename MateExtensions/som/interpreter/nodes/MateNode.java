package som.interpreter.nodes;

import som.interpreter.SArguments;
import som.interpreter.nodes.MateDispatch.MateDispatchMessageSend;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SReflectiveObject;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public abstract class MateNode extends ExpressionNode {
  @Child protected MateDispatch reflectiveDispatch;
  
  protected SMateEnvironment environment;
  
  public MateNode(final MateDispatch node, ExpressionNode wrappedNode) {
    super(node.getSourceSection());
    reflectiveDispatch = node;
  }
  
  public static MateNode createForGenericExpression(ExpressionNode node){
    MateDispatch dispatch; 
    switch (node.reflectiveOperation()){
      case Lookup:
          dispatch = MateDispatchMessageSend.create(node);
        break;
      default:
          dispatch = MateDispatch.create(node);
        break;
    }
    return MateNodeGen.create(dispatch, node);
  }
  
  /*public static MateNode createForPreevaluatedExpression(PreevaluatedExpression node){
    return MateNodeGen.create(MatePreEvaluatedDispatch.create(node),(ExpressionNode)node);
  }*/
  
  @Specialization(guards="hasReflectiveBehavior(frame)")
  public Object doMetaLevel(VirtualFrame frame){
    return reflectiveDispatch.executeDispatch(frame, environment);
  }
  
  @Specialization(guards="!hasReflectiveBehavior(frame)")
  public Object doBaseLevel(VirtualFrame frame) {
    return this.reflectiveDispatch.doBaselevel(frame);
  }
    
  protected boolean hasReflectiveBehavior(VirtualFrame frame){
    Object receiver = SArguments.rcvr(frame);
    //Need this check because of the possibility to receive primitive types 
    if (receiver instanceof SReflectiveObject){
      return  !((environment = ((SReflectiveObject)receiver).getEnvironment()) == null );
    } else {
      return false;
    }
  }
  
  public Node wrapIntoMateNode(){
    return this;
  }
}