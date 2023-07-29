package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.bdt.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;


@GenerateNodeFactory
@Primitive(className = "False", primitive = "not")
@Primitive(className = "True", primitive = "not")
@Primitive(selector = "not", receiverType = Boolean.class)
public abstract class NotMessageNode extends UnaryExpressionNode {
  @Specialization
  public static final boolean doNot(final VirtualFrame frame, final boolean receiver) {
    return !receiver;
  }
}
