package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.literals.BlockNode;
import trufflesom.interpreter.nodes.nary.BinaryExpressionNode;
import trufflesom.interpreter.nodes.specialized.AndMessageNode.AndOrSplzr;
import trufflesom.interpreter.nodes.specialized.AndMessageNodeFactory.AndBoolMessageNodeFactory;
import trufflesom.primitives.Primitive;
import trufflesom.primitives.Specializer;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SInvokable.SMethod;


@GenerateNodeFactory
@Primitive(selector = "and:", noWrapper = false, specializer = AndOrSplzr.class)
@Primitive(selector = "&&", noWrapper = false, specializer = AndOrSplzr.class)
public abstract class AndMessageNode extends BinaryExpressionNode {
  public static class AndOrSplzr extends Specializer<BinaryExpressionNode> {
    protected final NodeFactory<BinaryExpressionNode> boolFact;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public AndOrSplzr(final Primitive prim,
        final NodeFactory<BinaryExpressionNode> fact, final Universe universe) {
      this(prim, fact, (NodeFactory) AndBoolMessageNodeFactory.getInstance(), universe);
    }

    protected AndOrSplzr(final Primitive prim, final NodeFactory<BinaryExpressionNode> msgFact,
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
    public final BinaryExpressionNode create(final Object[] arguments,
        final ExpressionNode[] argNodes, final SourceSection section,
        final boolean eagerWrapper) {
      if (argNodes[1] instanceof BlockNode) {
        return fact.createNode(((BlockNode) argNodes[1]).getMethod(), section, argNodes[0],
            argNodes[1]);
      } else {
        assert arguments == null || arguments[1] instanceof Boolean;
        return boolFact.createNode(section, argNodes[0], argNodes[1]);
      }
    }
  }

  private final SInvokable      blockMethod;
  @Child private DirectCallNode blockValueSend;

  public AndMessageNode(final SMethod blockMethod, final SourceSection source) {
    super(source);
    this.blockMethod = blockMethod;
    blockValueSend = Truffle.getRuntime().createDirectCallNode(
        blockMethod.getCallTarget());
  }

  public AndMessageNode(final AndMessageNode copy) {
    super(copy.getSourceSection());
    blockMethod = copy.blockMethod;
    blockValueSend = copy.blockValueSend;
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

    public AndBoolMessageNode(final SourceSection source) {
      super(source);
    }

    @Specialization
    public final boolean doAnd(final VirtualFrame frame, final boolean receiver,
        final boolean argument) {
      return receiver && argument;
    }
  }
}
