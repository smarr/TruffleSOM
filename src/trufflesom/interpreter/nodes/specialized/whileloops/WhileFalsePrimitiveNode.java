package trufflesom.interpreter.nodes.specialized.whileloops;

import bd.primitives.Primitive;
import trufflesom.vmobjects.SBlock;


@Primitive(className = "Block", primitive = "whileFalse:", selector = "whileFalse:",
    receiverType = SBlock.class, noWrapper = true)
// TODO: need to check for the second argument, check WhileSplzr
public abstract class WhileFalsePrimitiveNode extends WhilePrimitiveNode {
  public WhileFalsePrimitiveNode() {
    super(false);
  }
}
