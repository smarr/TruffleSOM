package som.interpreter.nodes.nary;

import com.oracle.truffle.api.source.SourceSection;

import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.PreevaluatedExpression;


public abstract class EagerPrimitive extends ExpressionNode implements PreevaluatedExpression {

  protected EagerPrimitive(final SourceSection source) {
    super(source);
  }
}
