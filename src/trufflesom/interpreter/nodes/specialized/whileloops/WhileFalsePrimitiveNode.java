package trufflesom.interpreter.nodes.specialized.whileloops;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import bd.primitives.Primitive;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SObject;


@GenerateNodeFactory
@Primitive(className = "Block", primitive = "whileFalse:", selector = "whileFalse:",
    receiverType = SBlock.class, noWrapper = true)
// TODO: need to check for the second argument, check WhileSplzr
public abstract class WhileFalsePrimitiveNode extends WhilePrimitiveNode {
  public WhileFalsePrimitiveNode() {
    super(false);
  }

  @Specialization
  protected SObject doWhileConditionally(final VirtualFrame frame,
      final SBlock loopCondition, final SBlock loopBody) {
    return (SObject) whileNode.executeEvaluated(frame, loopCondition, loopBody);
  }
}
