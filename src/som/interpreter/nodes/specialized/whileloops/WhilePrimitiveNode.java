package som.interpreter.nodes.specialized.whileloops;

import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.vm.constants.Globals;
import som.vm.constants.Nil;
import som.vmobjects.SBlock;
import som.vmobjects.SObject;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;


public abstract class WhilePrimitiveNode extends BinaryExpressionNode {
  final boolean predicateBool;

  protected WhilePrimitiveNode(final boolean predicateBool) {
    super(null);
    this.predicateBool = predicateBool;
  }

  protected WhilePrimitiveNode(final WhilePrimitiveNode node) {
    this(node.predicateBool);
  }

  @Specialization
  protected SObject doWhileConditionally(final VirtualFrame frame,
      final SBlock loopCondition, final SBlock loopBody) {
    Object conditionResult = loopCondition.getMethod().invoke(loopCondition);
    boolean loopConditionResult = obj2bool(conditionResult);

    // TODO: this is a simplification, we don't cover the case receiver isn't a boolean
    while (loopConditionResult == predicateBool) {
      loopBody.getMethod().invoke(loopBody);
      conditionResult = loopCondition.getMethod().invoke(loopCondition);
      loopConditionResult = obj2bool(conditionResult);
    }
    return Nil.nilObject;
  }

  public abstract static class WhileTruePrimitiveNode extends WhilePrimitiveNode {
    public WhileTruePrimitiveNode() { super(true); }
  }

  private boolean obj2bool(final Object o) {
    if (o instanceof Boolean) {
      return (boolean) o;
    } else if (o == Globals.trueObject) {
      return true;
    } else {
      assert o == Globals.falseObject;
      return false;
    }
  }
}
