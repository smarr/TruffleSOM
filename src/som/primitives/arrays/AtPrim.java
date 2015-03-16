package som.primitives.arrays;

import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.vmobjects.SArray;

import com.oracle.truffle.api.dsl.Specialization;

public abstract class AtPrim extends BinaryExpressionNode {
  @Specialization
  public final Object doArray(final Object[] receiver, final long idx) {
    return SArray.get(receiver, idx);
  }
}
