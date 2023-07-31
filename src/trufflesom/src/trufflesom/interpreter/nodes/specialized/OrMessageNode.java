package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;

import trufflesom.bdt.primitives.Primitive;
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
@Primitive(selector = "or:", specializer = OrSplzr.class)
@Primitive(selector = "||", specializer = OrSplzr.class)
public abstract class OrMessageNode extends BinaryMsgExprNode {
  public static final class OrSplzr extends AndOrSplzr {
    @SuppressWarnings({"unchecked", "rawtypes"})
    public OrSplzr(final Primitive prim, final NodeFactory<ExpressionNode> fact) {
      super(prim, fact, (NodeFactory) OrBoolMessageNodeFactory.getInstance());
    }
  }

  protected final SInvokable    blockMethod;
  @Child private DirectCallNode blockValueSend;

  public OrMessageNode(final SMethod blockMethod) {
    this.blockMethod = blockMethod;
    blockValueSend = Truffle.getRuntime().createDirectCallNode(
        blockMethod.getCallTarget());
  }

  @Override
  public SSymbol getSelector() {
    if (getSourceChar(0) == '|') {
      return SymbolTable.symbolFor("||");
    }
    return SymbolTable.symbolFor("or:");
  }

  @Specialization(guards = "argument.getMethod() == blockMethod")
  public final boolean doOr(final boolean receiver, final SBlock argument) {
    if (receiver) {
      return true;
    } else {
      return (boolean) blockValueSend.call(new Object[] {argument});
    }
  }
}
