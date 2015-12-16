package som.interpreter.nodes.nary;

import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.ExpressionWithReceiverNode;
import som.interpreter.nodes.PreevaluatedExpression;
import som.vm.constants.ReflectiveOp;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

@NodeChild(value = "receiver", type = ExpressionNode.class)
public abstract class UnaryExpressionNode extends ExpressionWithReceiverNode
    implements PreevaluatedExpression {

  public abstract ExpressionNode getReceiver();
  
  public UnaryExpressionNode(final SourceSection source) {
    super(source);
  }

  // For nodes that are not representing source code
  public UnaryExpressionNode() { super(null); }

  public abstract Object executeEvaluated(final VirtualFrame frame,
      final Object receiver);

  @Override
  public final Object doPreEvaluated(final VirtualFrame frame,
      final Object[] arguments) {
    return executeEvaluated(frame, arguments[0]);
  }
  
  @Override
  public Object evaluateReceiver(VirtualFrame frame){
    return this.getReceiver().executeGeneric(frame);
  }
  
  public Object[] evaluateArguments(final VirtualFrame frame){
    Object[] arguments = new Object[1];
    arguments[0] = this.getReceiver().executeGeneric(frame);
    return arguments; 
  }
  
  public ReflectiveOp reflectiveOperation(){
    return ReflectiveOp.MessageLookup;
  }
}
