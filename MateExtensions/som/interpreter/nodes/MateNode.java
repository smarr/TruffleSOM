package som.interpreter.nodes;

import som.interpreter.nodes.MateDispatch.MatePreEvaluatedDispatch;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SObject;
import som.vmobjects.SReflectiveObject;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public class MateNode extends ExpressionNode {
  @Child protected MateDispatch reflectiveDispatch;
  
  protected Object[] arguments;
  
  private MateNode(final MateDispatch node) {
    super(node.getSourceSection());
    reflectiveDispatch = node;
  }
  
  public static MateNode createForGenericExpression(ExpressionNode node){
    return new MateNode(MateDispatch.create(node));
  }
  
  public static MateNode createForPreevaluatedExpression(PreevaluatedExpression node){
    return new MateNode(MatePreEvaluatedDispatch.create(node));
  }
  
  public final boolean hasReflectiveBehavior(VirtualFrame frame){
    arguments = this.reflectiveDispatch.evaluateArguments(frame);
    Object receiver = arguments[0];
    //Need this check because of the possibility to receive primitive types 
    if (receiver instanceof SReflectiveObject)
      return ((SReflectiveObject)receiver).hasReflectiveBehaviorFor(this.reflectiveDispatch.reflectiveOperation());
    else
      return false;
  }
  
  public Object executeGeneric(VirtualFrame frame){
    boolean reimplementedInMOP = this.hasReflectiveBehavior(frame);
    return reflectiveDispatch.executeDispatch(frame, reimplementedInMOP, arguments);
  }
  
  public Node wrapIntoMateNode(){
    return this;
  }
}