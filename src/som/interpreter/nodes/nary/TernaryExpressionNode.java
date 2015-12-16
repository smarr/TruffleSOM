package som.interpreter.nodes.nary;

import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.ExpressionWithReceiverNode;
import som.interpreter.nodes.PreevaluatedExpression;
import som.vm.constants.ReflectiveOp;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;


@NodeChildren({
  @NodeChild(value = "receiver",  type = ExpressionNode.class),
  @NodeChild(value = "firstArg",  type = ExpressionNode.class),
  @NodeChild(value = "secondArg", type = ExpressionNode.class)})
public abstract class TernaryExpressionNode extends ExpressionWithReceiverNode
    implements PreevaluatedExpression {

  public abstract ExpressionNode getReceiver();
  public abstract ExpressionNode getFirstArg();
  public abstract ExpressionNode getSecondArg();
  
  public TernaryExpressionNode(final SourceSection sourceSection) {
    super(sourceSection);
  }

  public TernaryExpressionNode() { this(null); }

  public abstract Object executeEvaluated(final VirtualFrame frame,
      final Object receiver, final Object firstArg, final Object secondArg);

  @Override
  public final Object doPreEvaluated(final VirtualFrame frame,
      final Object[] arguments) {
    return executeEvaluated(frame, arguments[0], arguments[1], arguments[2]);
  }
  
  public Object[] evaluateArguments(final VirtualFrame frame){
    Object[] arguments = new Object[3];
    arguments[0] = this.getReceiver().executeGeneric(frame);
    arguments[1] = this.getFirstArg().executeGeneric(frame);
    arguments[2] = this.getSecondArg().executeGeneric(frame);
    return arguments; 
  }
  
  public ReflectiveOp reflectiveOperation(){
    return ReflectiveOp.MessageLookup;
  }
}
