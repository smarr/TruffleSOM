package som.interpreter.nodes.literals;

import som.vmobjects.SArray;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

public final class ArrayLiteralNode extends LiteralNode {

  private final SArray values;

  public ArrayLiteralNode(SArray values, final SourceSection source) {
    super(source);
    this.values = values;
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    return values;
  }
}
