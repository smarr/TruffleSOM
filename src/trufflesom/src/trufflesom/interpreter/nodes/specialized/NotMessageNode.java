package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.bytecode.OperationProxy.Proxyable;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import trufflesom.bdt.primitives.Primitive;
import trufflesom.interpreter.Method.OpBuilder;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;


@Proxyable
@GenerateNodeFactory
@Primitive(className = "False", primitive = "not")
@Primitive(className = "True", primitive = "not")
@Primitive(selector = "not", receiverType = Boolean.class)
public abstract class NotMessageNode extends UnaryExpressionNode {
  @Specialization
  public static final boolean doNot(final boolean receiver) {
    return !receiver;
  }

  @Override
  public void constructOperation(final OpBuilder opBuilder, boolean resultUsed) {
    opBuilder.dsl.beginNotMessage();
    getReceiver().accept(opBuilder);
    opBuilder.dsl.endNotMessage();
  }
}
