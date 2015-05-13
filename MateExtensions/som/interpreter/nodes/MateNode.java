package som.interpreter.nodes;

import som.interpreter.nodes.MateDispatch.MatePreEvaluatedDispatch;
import com.oracle.truffle.api.frame.VirtualFrame;

public class MateNode extends ExpressionNode {
  @Child protected MateDispatch reflectiveDispatch;
  
  protected Object[] arguments;
  
  public MateNode(final MateDispatch node) {
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
    //this.evaluateArguments(frame);
    return false;
    //return this.arguments[0].hasReflectiveBehavior();
    //receiver.hasReflectiveBehavior();
  }
  
  public Object executeGeneric(VirtualFrame frame){
    boolean reimplementedInMOP = this.hasReflectiveBehavior(frame);
    return reflectiveDispatch.executeDispatch(frame, reimplementedInMOP, this.reflectiveDispatch.evaluateArguments(frame));
  }
}