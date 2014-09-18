package som.interpreter.nodes.specialized.whileloops;

import som.interpreter.nodes.literals.BlockNode.BlockNodeWithContext;
import som.vmobjects.SBlock;
import som.vmobjects.SObject;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;


public abstract class WhileWithStaticBlocksNode extends AbstractWhileNode {
  @Child protected BlockNodeWithContext receiver;
  @Child protected BlockNodeWithContext argument;

  private WhileWithStaticBlocksNode(final BlockNodeWithContext receiver,
      final BlockNodeWithContext argument, final SBlock rcvr, final SBlock arg,
      final boolean predicateBool, final SourceSection source) {
    super(rcvr, arg, predicateBool, source);
    this.receiver = receiver;
    this.argument = argument;
  }

  @Override
  public final Object executeGeneric(final VirtualFrame frame) {
    SBlock rcvr = receiver.executeSBlock(frame);
    SBlock arg  = argument.executeSBlock(frame);
    return executeEvaluated(frame, rcvr, arg);
  }

  @Override
  protected final SObject doWhileConditionally(final VirtualFrame frame,
      final SBlock loopCondition,
      final SBlock loopBody) {
    return doWhileUnconditionally(frame, loopCondition, loopBody);
  }

  public static final class WhileTrueStaticBlocksNode extends WhileWithStaticBlocksNode {
    public WhileTrueStaticBlocksNode(final BlockNodeWithContext receiver,
        final BlockNodeWithContext argument, final SBlock rcvr, final SBlock arg,
        final SourceSection source) {
      super(receiver, argument, rcvr, arg, true, source);
    }
  }
}
