package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.interpreter.nodes.nary.BinaryExpressionNode;


@GenerateNodeFactory
public abstract class AndBoolMessageNode extends BinaryExpressionNode {
  @Specialization
  public final boolean doAnd(final VirtualFrame frame, final boolean receiver,
      final boolean argument) {
    return receiver && argument;
  }
}
