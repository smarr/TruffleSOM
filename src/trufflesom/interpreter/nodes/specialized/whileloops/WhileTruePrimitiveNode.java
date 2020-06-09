package trufflesom.interpreter.nodes.specialized.whileloops;

import bd.primitives.Primitive;
import trufflesom.vmobjects.SBlock;


@Primitive(className = "Block", primitive = "whileTrue:", selector = "whileTrue:",
    receiverType = SBlock.class, noWrapper = true)
// TODO: need to check for the second argument, check WhileSplzr
public abstract class WhileTruePrimitiveNode extends WhilePrimitiveNode {
  public WhileTruePrimitiveNode() {
    super(true);
  }
}
