package trufflesom.primitives.arithmetic;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import bdt.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;


@GenerateNodeFactory
@Primitive(className = "Double", primitive = "sin")
public abstract class SinPrim extends UnaryExpressionNode {
  @Specialization
  public final double doSin(final double rcvr) {
    return Math.sin(rcvr);
  }
}
