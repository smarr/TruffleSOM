package som.primitives.arrays;

import som.interpreter.nodes.nary.UnaryExpressionNode;

import com.oracle.truffle.api.dsl.Specialization;

public abstract class CopyPrim extends UnaryExpressionNode {
  @Specialization
  public final Object[] doObjectArray(final Object[] receiver) {
    return receiver.clone();
  }
}
