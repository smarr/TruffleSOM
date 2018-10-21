package som.interpreter.nodes.specialized.whileloops;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;

import som.interpreter.Invokable;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.vm.constants.Nil;
import som.vmobjects.SBlock;
import som.vmobjects.SObject;


public abstract class AbstractWhileNode extends BinaryExpressionNode {
  @Child protected DirectCallNode conditionValueSend;
  @Child protected DirectCallNode bodyValueSend;

  protected final boolean predicateBool;

  public AbstractWhileNode(final SBlock rcvr, final SBlock arg,
      final boolean predicateBool, final SourceSection source) {
    super(source);

    CallTarget callTargetCondition = rcvr.getMethod().getCallTarget();
    conditionValueSend = Truffle.getRuntime().createDirectCallNode(
        callTargetCondition);

    CallTarget callTargetBody = arg.getMethod().getCallTarget();
    bodyValueSend = Truffle.getRuntime().createDirectCallNode(
        callTargetBody);

    this.predicateBool = predicateBool;
  }

  @Override
  public final Object executeEvaluated(final VirtualFrame frame,
      final Object rcvr, final Object arg) {
    return doWhileConditionally((SBlock) rcvr, (SBlock) arg);
  }

  protected final SObject doWhileUnconditionally(final SBlock loopCondition,
      final SBlock loopBody) {
    long iterationCount = 0;

    boolean loopConditionResult =
        (boolean) conditionValueSend.call(new Object[] {loopCondition});

    try {
      // TODO: this is a simplification, we don't cover the case receiver isn't a boolean
      while (loopConditionResult == predicateBool) {
        bodyValueSend.call(new Object[] {loopBody});
        loopConditionResult = (boolean) conditionValueSend.call(new Object[] {loopCondition});

        if (CompilerDirectives.inInterpreter()) {
          iterationCount++;
        }
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount(iterationCount);
      }
    }
    return Nil.nilObject;
  }

  protected abstract SObject doWhileConditionally(SBlock loopCondition, SBlock loopBody);

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
}
