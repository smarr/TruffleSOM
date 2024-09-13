package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.bytecode.OperationProxy.Proxyable;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import trufflesom.interpreter.Method.OpBuilder;
import trufflesom.interpreter.nodes.nary.BinaryExpressionNode;


@Proxyable
@GenerateNodeFactory
public abstract class OrBoolMessageNode extends BinaryExpressionNode {
  @Specialization
  public static final boolean doOr(final boolean receiver, final boolean argument) {
    return receiver || argument;
  }

  @Override
  public void constructOperation(final OpBuilder opBuilder, boolean resultUsed) {
    opBuilder.dsl.beginOrBoolMessage();
    getReceiver().accept(opBuilder);
    getArgument().accept(opBuilder);
    opBuilder.dsl.endOrBoolMessage();
  }
}
