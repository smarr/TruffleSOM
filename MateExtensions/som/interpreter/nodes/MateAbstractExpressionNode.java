/*package som.interpreter.nodes;

import som.vm.MateUniverse;
import som.vm.constants.Nil;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SReflectiveObject;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.ShortCircuit;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

@NodeChildren({
@NodeChild(value = "receiver", type = MateReceiverNode.class),
@NodeChild(value = "semantics", type = MateBehaviorNode.class, executeWith = "receiver")})
public abstract class MateAbstractExpressionNode extends ExpressionNode {
  public MateAbstractExpressionNode(SourceSection sourceSection) {
    super(sourceSection);
  }
  
  public abstract MateReceiverNode getReceiver();

  @Child protected MateReflectiveDispatch mateDispatch;
  
  protected ExpressionNode getSOMWrappedNode(){
    return this.getReceiver().getSOMNode();
  }  
  
  @ShortCircuit("semantics")
  boolean needsSemantics(SReflectiveObject receiver) {
    return receiver.getEnvironment() != Nil.nilObject;
  }
  @ShortCircuit("semantics")
  boolean needsSemantics(Object receiver) {
    return false;
  }
  
  @Specialization(guards = "executingMeta() || (environment == null || !hasSemantics)")
  public Object executeSomExpression(VirtualFrame frame, Object receiver, boolean hasSemantics, Object environment) {
    return this.getReceiver().getSOMNode().executeGeneric(frame);
  }
  
  @Specialization
  public Object tryMateReflectiveOperation(VirtualFrame frame, Object receiver, boolean hasSemantics, SMateEnvironment environment) {
    Object[] arguments = this.getReceiver().getArguments();  
    return mateDispatch.executeDispatch(frame, environment);
      
  }
  
  public static boolean executingMeta(){
    return MateUniverse.current().executingMeta();
  }
}
*/