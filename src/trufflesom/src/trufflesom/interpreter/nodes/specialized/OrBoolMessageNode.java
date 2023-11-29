package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import trufflesom.interpreter.nodes.nary.BinaryExpressionNode;


@GenerateNodeFactory
public abstract class OrBoolMessageNode extends BinaryExpressionNode {
  @Specialization
  public static final boolean doOr(final boolean receiver, final boolean argument) {
    return receiver || argument;
  }
}
