package som.interpreter.nodes;

import som.interpreter.SArguments;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public abstract class MateAbstractReceiverNode extends Node {
  
  public abstract Object executeGeneric(VirtualFrame frame);
  
  public MateAbstractReceiverNode(){}  
  
  public static class MateReceiverNode extends MateAbstractReceiverNode {
    public Object executeGeneric(VirtualFrame frame){
      return SArguments.rcvr(frame);
    }
  }
  
  public static class MateReceiverExpressionNode extends MateAbstractReceiverNode {
    @Child protected ExpressionWithReceiverNode wrappedNode;
    
    public MateReceiverExpressionNode(ExpressionWithReceiverNode node){
      wrappedNode = node;
    }
    
    public ExpressionWithReceiverNode getSOMNode(){
      return this.wrappedNode;
    }
    
    public Object executeGeneric(VirtualFrame frame){
      return this.getSOMNode().evaluateReceiver(frame);
    }
  }
}
