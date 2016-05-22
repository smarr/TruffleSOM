package som.primitives.arithmetic;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import som.interpreter.nodes.nary.UnaryExpressionNode;


@GenerateNodeFactory
public abstract class CosPrim extends UnaryExpressionNode {

  public CosPrim() { super(null); }

  @Specialization
  public final double doCos(final double rcvr) {
    return Math.cos(rcvr);
  }
}
