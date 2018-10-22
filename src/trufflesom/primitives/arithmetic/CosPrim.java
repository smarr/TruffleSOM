package trufflesom.primitives.arithmetic;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import bd.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;


@GenerateNodeFactory
@Primitive(className = "Double", primitive = "cos")
public abstract class CosPrim extends UnaryExpressionNode {
  @Specialization
  public final double doCos(final double rcvr) {
    return Math.cos(rcvr);
  }
}
