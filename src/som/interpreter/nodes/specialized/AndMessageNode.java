package som.interpreter.nodes.specialized;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.source.SourceSection;

import bd.primitives.Primitive;
import bd.primitives.Specializer;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.literals.BlockNode;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.EagerlySpecializableNode;
import som.interpreter.nodes.specialized.AndMessageNode.AndOrSplzr;
import som.interpreter.nodes.specialized.AndMessageNodeFactory.AndBoolMessageNodeFactory;
import som.vm.Universe;
import som.vmobjects.SBlock;
import som.vmobjects.SInvokable;
import som.vmobjects.SInvokable.SMethod;
import som.vmobjects.SSymbol;


@GenerateNodeFactory
@Primitive(selector = "and:", noWrapper = false, specializer = AndOrSplzr.class)
@Primitive(selector = "&&", noWrapper = false, specializer = AndOrSplzr.class)
public abstract class AndMessageNode extends BinaryExpressionNode {
  public static class AndOrSplzr extends Specializer<Universe, ExpressionNode, SSymbol> {
    protected final NodeFactory<BinaryExpressionNode> boolFact;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public AndOrSplzr(final Primitive prim,
        final NodeFactory<ExpressionNode> fact, final Universe universe) {
      this(prim, fact, (NodeFactory) AndBoolMessageNodeFactory.getInstance(), universe);
    }

    protected AndOrSplzr(final Primitive prim, final NodeFactory<ExpressionNode> msgFact,
        final NodeFactory<BinaryExpressionNode> boolFact, final Universe universe) {
      super(prim, msgFact, universe);
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
        final ExpressionNode[] argNodes, final SourceSection section,
        final boolean eagerWrapper) {
      EagerlySpecializableNode node;
      if (argNodes[1] instanceof BlockNode) {
        node = (EagerlySpecializableNode) fact.createNode(
            ((BlockNode) argNodes[1]).getMethod(), argNodes[0], argNodes[1]);
      } else {
        assert arguments == null || arguments[1] instanceof Boolean;
        node = boolFact.createNode(argNodes[0], argNodes[1]);
      }
      node.initialize(section, eagerWrapper);
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

  @GenerateNodeFactory
  public abstract static class AndBoolMessageNode extends BinaryExpressionNode {
    @Specialization
    public final boolean doAnd(final VirtualFrame frame, final boolean receiver,
        final boolean argument) {
      return receiver && argument;
    }
  }
}
