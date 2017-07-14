package som.primitives.arithmetic;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;

import bd.primitives.Primitive;
import som.interpreter.nodes.nary.UnaryExpressionNode;


@GenerateNodeFactory
@Primitive(className = "Double", primitive = "sin")
public abstract class SinPrim extends UnaryExpressionNode {

  public SinPrim(final SourceSection source) {
    super(source);
  }

  @Specialization
  public final double doSin(final double rcvr) {
    return Math.sin(rcvr);
  }
}
