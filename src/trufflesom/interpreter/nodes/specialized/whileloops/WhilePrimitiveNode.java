package trufflesom.interpreter.nodes.specialized.whileloops;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node.Child;
import com.oracle.truffle.api.object.DynamicObject;

import trufflesom.interpreter.nodes.nary.BinaryExpressionNode.BinarySystemOperation;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SBlock;


public abstract class WhilePrimitiveNode extends BinarySystemOperation {
  final boolean predicateBool;

  @Child protected WhileCache whileNode;

  protected WhilePrimitiveNode(final boolean predicateBool) {
    this.predicateBool = predicateBool;

  }

  @Override
  public WhilePrimitiveNode initialize(final Universe universe) {
    super.initialize(universe);
    this.whileNode = WhileCacheNodeGen.create(predicateBool, universe, null, null)
                                      .initialize(sourceSection);
    return this;
  }

  @Specialization
  protected DynamicObject doWhileConditionally(final VirtualFrame frame,
      final SBlock loopCondition, final SBlock loopBody) {
    return (DynamicObject) whileNode.executeEvaluated(frame, loopCondition, loopBody);
  }
}
