package som.primitives.arrays;

import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.vmobjects.SArray;

import com.oracle.truffle.api.dsl.Specialization;

public abstract class AtPutPrim extends TernaryExpressionNode {

  @Specialization
  public final Object doArray(final Object[] receiver, final long index,
      final Object value) {
    SArray.set(receiver, index, value);
    return value;
  }
}
