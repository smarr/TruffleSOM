package trufflesom.interpreter.nodes.specialized.whileloops;

import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.interpreter.nodes.literals.BlockNode;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SObject;


public abstract class WhileWithStaticBlocksNode extends AbstractWhileNode {
  @Child protected BlockNode receiver;
  @Child protected BlockNode argument;

  private WhileWithStaticBlocksNode(final BlockNode receiver, final BlockNode argument,
      final SBlock rcvr, final SBlock arg, final boolean predicateBool) {
    super(rcvr, arg, predicateBool);
    this.receiver = receiver;
    this.argument = argument;
  }

  @Override
  public final Object executeGeneric(final VirtualFrame frame) {
    SBlock rcvr = (SBlock) receiver.executeGeneric(frame);
    SBlock arg = (SBlock) argument.executeGeneric(frame);
    return executeEvaluated(frame, rcvr, arg);
  }

  @Override
  protected final SObject doWhileConditionally(final SBlock loopCondition,
      final SBlock loopBody) {
    return doWhileUnconditionally(loopCondition, loopBody);
  }

  public static final class WhileTrueStaticBlocksNode extends WhileWithStaticBlocksNode {
    public WhileTrueStaticBlocksNode(final BlockNode receiver,
        final BlockNode argument, final SBlock rcvr, final SBlock arg) {
      super(receiver, argument, rcvr, arg, true);
    }
  }

  public static final class WhileFalseStaticBlocksNode extends WhileWithStaticBlocksNode {
    public WhileFalseStaticBlocksNode(final BlockNode receiver,
        final BlockNode argument, final SBlock rcvr, final SBlock arg) {
      super(receiver, argument, rcvr, arg, false);
    }
  }
}
