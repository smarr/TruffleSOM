package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;

import bd.primitives.Primitive;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.nary.BinaryExpressionNode;
import trufflesom.interpreter.nodes.specialized.AndMessageNode.AndOrSplzr;
import trufflesom.interpreter.nodes.specialized.OrMessageNode.OrSplzr;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SInvokable;


@GenerateNodeFactory
@Primitive(selector = "or:", noWrapper = true, specializer = OrSplzr.class)
@Primitive(selector = "||", noWrapper = true, specializer = OrSplzr.class)
public abstract class OrMessageNode extends BinaryExpressionNode {
  public static final class OrSplzr extends AndOrSplzr {
    @SuppressWarnings({"unchecked", "rawtypes"})
    public OrSplzr(final Primitive prim, final NodeFactory<ExpressionNode> fact) {
      super(prim, fact, (NodeFactory) OrBoolMessageNodeFactory.getInstance());
    }
  }

  public static final DirectCallNode callNode(final SBlock block) {
    return Truffle.getRuntime().createDirectCallNode(block.getMethod().getCallTarget());
  }

  @Specialization(guards = "argument.getMethod() == blockMethod")
  public final boolean doOr(final boolean receiver, final SBlock argument,
      @Cached("argument.getMethod()") final SInvokable blockMethod,
      @Cached("callNode(argument)") final DirectCallNode send) {
    if (receiver) {
      return true;
    } else {
      return (boolean) send.call(new Object[] {argument});
    }
  }
}
