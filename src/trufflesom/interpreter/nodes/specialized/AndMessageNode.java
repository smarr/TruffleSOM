package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.source.SourceSection;

import bd.primitives.Primitive;
import bd.primitives.Specializer;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.literals.BlockNode;
import trufflesom.interpreter.nodes.nary.BinaryExpressionNode;
import trufflesom.interpreter.nodes.nary.BinaryMsgExprNode;
import trufflesom.interpreter.nodes.specialized.AndMessageNode.AndOrSplzr;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SSymbol;


@GenerateNodeFactory
@Primitive(selector = "and:", specializer = AndOrSplzr.class)
@Primitive(selector = "&&", specializer = AndOrSplzr.class)
public abstract class AndMessageNode extends BinaryMsgExprNode {
  public static class AndOrSplzr extends Specializer<ExpressionNode, SSymbol> {
    protected final NodeFactory<BinaryExpressionNode> boolFact;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public AndOrSplzr(final Primitive prim, final NodeFactory<ExpressionNode> fact) {
      this(prim, fact, (NodeFactory) AndBoolMessageNodeFactory.getInstance());
    }

    protected AndOrSplzr(final Primitive prim, final NodeFactory<ExpressionNode> msgFact,
        final NodeFactory<BinaryExpressionNode> boolFact) {
      super(prim, msgFact);
      this.boolFact = boolFact;
    }

    @Override
    public final boolean matches(final Object[] args, final ExpressionNode[] argNodes) {
      // XXX: this is the case when doing parse-time specialization
      if (args == null) {
        return true;
      }

      return args[0] instanceof Boolean
          && (args[1] instanceof Boolean || argNodes[1] instanceof BlockNode);
    }

    @Override
    public final ExpressionNode create(final Object[] arguments,
        final ExpressionNode[] argNodes, final SourceSection section) {
      ExpressionNode node;
      if (argNodes[1] instanceof BlockNode) {
        node = fact.createNode(
            ((BlockNode) argNodes[1]).getMethod(), argNodes[0], argNodes[1]);
      } else {
        assert arguments == null || arguments[1] instanceof Boolean;
        node = boolFact.createNode(argNodes[0], argNodes[1]);
      }
      node.initialize(section);
      return node;
    }
  }

  private final SInvokable      blockMethod;
  @Child private DirectCallNode blockValueSend;

  public AndMessageNode(final SMethod blockMethod) {
    this.blockMethod = blockMethod;
    blockValueSend = Truffle.getRuntime().createDirectCallNode(
        blockMethod.getCallTarget());
  }

  protected final boolean isSameBlock(final SBlock argument) {
    return argument.getMethod() == blockMethod;
  }

  @Specialization(guards = "isSameBlock(argument)")
  public final boolean doAnd(final boolean receiver, final SBlock argument) {
    if (receiver == false) {
      return false;
    } else {
      return (boolean) blockValueSend.call(new Object[] {argument});
    }
  }

  @Override
  public SSymbol getSelector() {
    // if (getSourceChar(0) == '&') {
    // return SymbolTable.symbolFor("&&");
    // }
    return SymbolTable.symbolFor("and:");
  }
}
