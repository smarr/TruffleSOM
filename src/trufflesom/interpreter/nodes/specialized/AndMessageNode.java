package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
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
import trufflesom.interpreter.nodes.nary.EagerlySpecializableNode;
import trufflesom.interpreter.nodes.specialized.AndMessageNode.AndOrSplzr;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SSymbol;


@GenerateNodeFactory
@Primitive(selector = "and:", noWrapper = false, specializer = AndOrSplzr.class)
@Primitive(selector = "&&", noWrapper = false, specializer = AndOrSplzr.class)
public abstract class AndMessageNode extends BinaryExpressionNode {
  public static class AndOrSplzr extends Specializer<Universe, ExpressionNode, SSymbol> {
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
        final ExpressionNode[] argNodes, final SourceSection section,
        final boolean eagerWrapper, final Universe universe) {
      EagerlySpecializableNode node;
      if (argNodes[1] instanceof BlockNode) {
        node = (EagerlySpecializableNode) fact.createNode(
            eagerWrapper ? null : argNodes[0],
            eagerWrapper ? null : argNodes[1]);
      } else {
        assert arguments == null || arguments[1] instanceof Boolean;
        node = boolFact.createNode(
            eagerWrapper ? null : argNodes[0],
            eagerWrapper ? null : argNodes[1]);
      }
      node.initialize(section, eagerWrapper);
      return node;
    }
  }

  public static final DirectCallNode callNode(final SBlock block) {
    return Truffle.getRuntime().createDirectCallNode(block.getMethod().getCallTarget());
  }

  @Specialization(guards = "argument.getMethod() == blockMethod")
  public final boolean doAnd(final boolean receiver, final SBlock argument,
      @Cached("argument.getMethod()") final SInvokable blockMethod,
      @Cached("callNode(argument)") final DirectCallNode send) {
    if (receiver == false) {
      return false;
    } else {
      return (boolean) send.call(new Object[] {argument});
    }
  }
}
