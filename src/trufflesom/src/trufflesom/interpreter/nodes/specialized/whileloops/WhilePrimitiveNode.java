package trufflesom.interpreter.nodes.specialized.whileloops;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;

import trufflesom.interpreter.nodes.nary.BinaryExpressionNode;
import trufflesom.interpreter.operations.copied.ArrayDoOp;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SObject;


public abstract class WhilePrimitiveNode extends BinaryExpressionNode {
  public static final int INLINE_CACHE_SIZE = 6;

  private static boolean obj2bool(final Object o) {
    if (o instanceof Boolean) {
      return (boolean) o;
    } else {
      throw new IllegalStateException(
          "This should not happen! There are no objects that are booleans, and we only support booleans.");
    }
  }

  protected static final SObject doWhileCached(final SBlock loopCondition,
      final SBlock loopBody, final DirectCallNode conditionNode,
      final DirectCallNode bodyNode, final boolean predicateBool, final Node self) {
    long iterationCount = 0;

    boolean loopConditionResult =
        (boolean) conditionNode.call(new Object[] {loopCondition});

    try {
      // TODO: this is a simplification, we don't cover the case receiver isn't a boolean
      while (loopConditionResult == predicateBool) {
        bodyNode.call(new Object[] {loopBody});
        loopConditionResult = (boolean) conditionNode.call(new Object[] {loopCondition});

        if (CompilerDirectives.inInterpreter()) {
          iterationCount++;
        }
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        ArrayDoOp.reportLoopCount(self, iterationCount);
      }
    }
    return Nil.nilObject;
  }

  protected static final SObject doWhileUncachedAndUncounted(final SBlock loopCondition,
      final SBlock loopBody, final boolean predicateBool) {
    Object conditionResult = loopCondition.getMethod().invoke(new Object[] {loopCondition});

    // TODO: this is a simplification, we don't cover the case receiver isn't a boolean
    boolean loopConditionResult = obj2bool(conditionResult);

    // TODO: this is a simplification, we don't cover the case receiver isn't a boolean
    while (loopConditionResult == predicateBool) {
      loopBody.getMethod().invoke(new Object[] {loopBody});
      conditionResult = loopCondition.getMethod().invoke(new Object[] {loopCondition});
      loopConditionResult = obj2bool(conditionResult);
    }
    return Nil.nilObject;
  }
}
