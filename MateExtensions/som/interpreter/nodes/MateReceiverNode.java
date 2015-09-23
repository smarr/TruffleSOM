package som.interpreter.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;


public class MateReceiverNode extends Node {
  @Child protected ExpressionWithReceiverNode wrappedNode;
  
  public MateReceiverNode(ExpressionWithReceiverNode node){
    wrappedNode = node;
  }
  
  public ExpressionWithReceiverNode getSOMNode(){
    return this.wrappedNode;
  }
  
  public Object[] getArguments(){
    return null;
  }
  
  public Object executeGeneric(VirtualFrame frame){
    return this.getSOMNode().evaluateReceiver(frame);
  }
}
