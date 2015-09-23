package som.interpreter.nodes.specialized.whileloops;

import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.literals.IntegerLiteralNode;
import som.vm.NotYetImplementedException;
import som.vmobjects.SBlock;
import som.vmobjects.SInvokable;
import som.vmobjects.SObject;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;


public final class WhileWithDynamicBlocksNode extends AbstractWhileNode {
  private final SInvokable conditionMethod;
  private final SInvokable bodyMethod;

  @Override
  /*Analyze what is the best to do for this case*/
  public ExpressionNode getReceiver(){
    return new IntegerLiteralNode(1,this.getSourceSection());
  }
  
  public final static WhileWithDynamicBlocksNode create(final SBlock rcvr,
      final SBlock arg, final boolean predicateBool) {
    return new WhileWithDynamicBlocksNode(rcvr, arg, predicateBool, null);
  }

  public WhileWithDynamicBlocksNode(final SBlock rcvr, final SBlock arg,
      final boolean predicateBool, final SourceSection source) {
    super(rcvr, arg, predicateBool, source);
    conditionMethod = rcvr.getMethod();
    bodyMethod = arg.getMethod();
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    CompilerAsserts.neverPartOfCompilation("WhileWithDynamicBlocksNode.generic");
    throw new NotYetImplementedException();
  }

  @Override
  protected SObject doWhileConditionally(final VirtualFrame frame,
      final SBlock loopCondition,
      final SBlock loopBody) {
    assert loopCondition.getMethod() == conditionMethod;
    assert loopBody.getMethod()      == bodyMethod;
    return doWhileUnconditionally(frame, loopCondition, loopBody);
  }
}
