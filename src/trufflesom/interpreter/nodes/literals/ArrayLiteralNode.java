package trufflesom.interpreter.nodes.literals;

import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.vmobjects.SArray;


public final class ArrayLiteralNode extends LiteralNode {

  private final SArray values;

  public ArrayLiteralNode(final SArray values) {
    this.values = values;
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    return values;
  }
}
