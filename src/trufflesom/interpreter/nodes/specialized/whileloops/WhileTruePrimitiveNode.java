package trufflesom.interpreter.nodes.specialized.whileloops;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import bd.primitives.Primitive;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SObject;


@GenerateNodeFactory
@Primitive(className = "Block", primitive = "whileTrue:", selector = "whileTrue:",
    receiverType = SBlock.class, noWrapper = true)
// TODO: need to check for the second argument, check WhileSplzr
public abstract class WhileTruePrimitiveNode extends WhilePrimitiveNode {
  public WhileTruePrimitiveNode() {
    super(true);
  }

  @Specialization
  protected SObject doWhileConditionally(final VirtualFrame frame,
      final SBlock loopCondition, final SBlock loopBody) {
    return (SObject) whileNode.executeEvaluated(frame, loopCondition, loopBody);
  }
}
