package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node.Child;

import bd.primitives.Primitive;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.nary.BinaryMsgExprNode;
import trufflesom.interpreter.nodes.specialized.AndMessageNode.AndOrSplzr;
import trufflesom.interpreter.nodes.specialized.OrMessageNode.OrSplzr;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SSymbol;


@GenerateNodeFactory
@Primitive(selector = "or:", noWrapper = true, specializer = OrSplzr.class)
@Primitive(selector = "||", noWrapper = true, specializer = OrSplzr.class)
public abstract class OrMessageNode extends BinaryMsgExprNode {
  public static final class OrSplzr extends AndOrSplzr {
    @SuppressWarnings({"unchecked", "rawtypes"})
    public OrSplzr(final Primitive prim, final NodeFactory<ExpressionNode> fact) {
      super(prim, fact, (NodeFactory) OrBoolMessageNodeFactory.getInstance());
    }
  }

  private final SInvokable      blockMethod;
  @Child private DirectCallNode blockValueSend;

  public OrMessageNode(final SMethod blockMethod) {
    this.blockMethod = blockMethod;
    blockValueSend = Truffle.getRuntime().createDirectCallNode(
        blockMethod.getCallTarget());
  }

  protected final boolean isSameBlock(final SBlock argument) {
    return argument.getMethod() == blockMethod;
  }

  @Override
  public SSymbol getSelector() {
    return SymbolTable.symbolFor("||");
  }

  @Specialization(guards = "isSameBlock(argument)")
  public final boolean doOr(final boolean receiver, final SBlock argument) {
    if (receiver) {
      return true;
    } else {
      return (boolean) blockValueSend.call(new Object[] {argument});
    }
  }
}
