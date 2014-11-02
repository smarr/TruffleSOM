package som.interpreter;

import som.interpreter.nodes.ExpressionNode;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;


public final class Primitive extends Invokable {

  public Primitive(final ExpressionNode primitive,
      final FrameDescriptor frameDescriptor,
      final FrameSlot frameOnStackMarker) {
    super(null, frameDescriptor, frameOnStackMarker, primitive);
  }

  @Override
  public String toString() {
    return "Primitive " + expressionOrSequence.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
  }
}
