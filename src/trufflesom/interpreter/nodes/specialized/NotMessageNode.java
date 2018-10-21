package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.primitives.Primitive;


@GenerateNodeFactory
@Primitive(className = "False", primitive = "not")
@Primitive(className = "True", primitive = "not")
@Primitive(selector = "not", receiverType = Boolean.class)
public abstract class NotMessageNode extends UnaryExpressionNode {
  public NotMessageNode(final SourceSection source) {
    super(source);
  }

  public NotMessageNode() {
    this(null);
  } // only for the primitive version

  @Specialization
  public final boolean doNot(final VirtualFrame frame, final boolean receiver) {
    return !receiver;
  }
}
