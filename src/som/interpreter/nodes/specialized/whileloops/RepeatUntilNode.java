package som.interpreter.nodes.specialized.whileloops;

import som.interpreter.Invokable;
import som.interpreter.nodes.literals.BlockNode;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.vm.constants.Nil;
import som.vmobjects.SBlock;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;


public class RepeatUntilNode extends BinaryExpressionNode {
  @Child protected BlockNode receiver;
  @Child protected BlockNode argument;

  @Child protected DirectCallNode conditionValueSend;
  @Child protected DirectCallNode bodyValueSend;

  public RepeatUntilNode(final BlockNode rcvrNode, final BlockNode argNode,
      final SBlock rcvr, final SBlock arg,
      final SourceSection source) {
    super(source);

    receiver = rcvrNode;
    CallTarget callTargetBody = rcvr.getMethod().getCallTarget();
    bodyValueSend = Truffle.getRuntime().createDirectCallNode(
        callTargetBody);

    argument = argNode;
    CallTarget callTargetCondition = arg.getMethod().getCallTarget();
    conditionValueSend = Truffle.getRuntime().createDirectCallNode(
        callTargetCondition);
  }

  @Override
  public final Object executeEvaluated(final VirtualFrame frame,
      final Object rcvr, final Object arg) {
    doLoop(frame, (SBlock) rcvr, (SBlock) arg);
    return Nil.nilObject;
  }

  private void doLoop(final VirtualFrame frame,
      final SBlock loopBody, final SBlock loopCondition) {
    long iterationCount = 0;

    try {
      boolean loopConditionResult;
      do  {
        bodyValueSend.call(frame, new Object[] {loopBody});
        loopConditionResult = (boolean) conditionValueSend.call(
            frame, new Object[] {loopCondition});

        if (CompilerDirectives.inInterpreter()) { iterationCount++; }
      } while (!loopConditionResult);
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount(iterationCount);
      }
    }
  }

  protected final void reportLoopCount(final long count) {
    CompilerAsserts.neverPartOfCompilation("reportLoopCount");
    Node current = getParent();
    while (current != null && !(current instanceof RootNode)) {
      current = current.getParent();
    }
    if (current != null) {
      ((Invokable) current).propagateLoopCountThroughoutLexicalScope(count);
    }
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    SBlock rcvr = receiver.executeSBlock(frame);
    SBlock arg  = argument.executeSBlock(frame);
    return executeEvaluated(frame, rcvr, arg);
  }
}
