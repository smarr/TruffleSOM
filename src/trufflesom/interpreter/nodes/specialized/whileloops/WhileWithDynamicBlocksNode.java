package trufflesom.interpreter.nodes.specialized.whileloops;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.vm.NotYetImplementedException;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SObject;


public final class WhileWithDynamicBlocksNode extends AbstractWhileNode {
  private final SInvokable conditionMethod;
  private final SInvokable bodyMethod;

  public static WhileWithDynamicBlocksNode create(final SBlock rcvr,
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
  protected SObject doWhileConditionally(final SBlock loopCondition, final SBlock loopBody) {
    assert loopCondition.getMethod() == conditionMethod;
    assert loopBody.getMethod() == bodyMethod;
    return doWhileUnconditionally(loopCondition, loopBody);
  }
}
