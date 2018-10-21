package trufflesom.interpreter.nodes.nary;

import com.oracle.truffle.api.source.SourceSection;

import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.PreevaluatedExpression;


public abstract class EagerPrimitive extends ExpressionNode implements PreevaluatedExpression {

  protected EagerPrimitive(final SourceSection source) {
    super(source);
  }
}
