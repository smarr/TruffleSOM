package som.interpreter.nodes;

import som.interpreter.SArguments;
import som.interpreter.nodes.MateDispatch.MatePreEvaluatedDispatch;
import som.vm.constants.ReflectiveOp;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SReflectiveObject;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public class MateNode extends ExpressionNode {
  protected MateDispatch reflectiveDispatch;
  
  protected Object receiver;
  protected SMateEnvironment environment;
  
  public MateNode(final MateDispatch node, ExpressionNode wrappedNode) {
    super(node.getSourceSection());
    reflectiveDispatch = node;
  }
  
  public static MateNode createForGenericExpression(ExpressionNode node){
    return new MateNode(MateDispatch.create(node), node);
  }
  
  public static MateNode createForPreevaluatedExpression(PreevaluatedExpression node){
    return new MateNode(MatePreEvaluatedDispatch.create(node),(ExpressionNode)node);
  }
  
  @Specialization(guards="hasReflectiveBehavior(frame)")
  public Object doMeta(VirtualFrame frame){
    return reflectiveDispatch.executeDispatch(frame, environment);
  }
  
  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return this.reflectiveDispatch.doBaselevel(frame);
  }
    
  boolean hasReflectiveBehavior(VirtualFrame frame){
    receiver = SArguments.rcvr(frame);
    //Need this check because of the possibility to receive primitive types 
    if (receiver instanceof SReflectiveObject){
      environment = ((SReflectiveObject)receiver).getEnvironment();
      return true; 
    } else {
      return false;
    }
  }
  
  public Node wrapIntoMateNode(){
    return this;
  }
}