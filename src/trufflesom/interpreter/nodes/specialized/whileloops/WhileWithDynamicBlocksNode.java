package trufflesom.interpreter.nodes.specialized.whileloops;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.vm.NotYetImplementedException;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SObject;


public final class WhileWithDynamicBlocksNode extends AbstractWhileNode {
  private final SInvokable conditionMethod;
  private final SInvokable bodyMethod;

  public static WhileWithDynamicBlocksNode create(final SBlock rcvr,
      final SBlock arg, final boolean predicateBool) {
    return new WhileWithDynamicBlocksNode(rcvr, arg, predicateBool);
  }

  public WhileWithDynamicBlocksNode(final SBlock rcvr, final SBlock arg,
      final boolean predicateBool) {
    super(rcvr, arg, predicateBool);
    conditionMethod = rcvr.getMethod();
    bodyMethod = arg.getMethod();
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    CompilerDirectives.transferToInterpreter();
    throw new NotYetImplementedException();
  }

  @Override
  protected SObject doWhileConditionally(final SBlock loopCondition, final SBlock loopBody) {
    assert loopCondition.getMethod() == conditionMethod;
    assert loopBody.getMethod() == bodyMethod;
    return doWhileUnconditionally(loopCondition, loopBody);
  }
}
