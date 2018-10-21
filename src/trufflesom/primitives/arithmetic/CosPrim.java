package trufflesom.primitives.arithmetic;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.primitives.Primitive;


@GenerateNodeFactory
@Primitive(className = "Double", primitive = "cos")
public abstract class CosPrim extends UnaryExpressionNode {

  public CosPrim(final SourceSection source) {
    super(source);
  }

  @Specialization
  public final double doCos(final double rcvr) {
    return Math.cos(rcvr);
  }
}
