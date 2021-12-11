package trufflesom.interpreter.nodes;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;

import bd.primitives.Specializer;
import bd.primitives.nodes.PreevaluatedExpression;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.primitives.Primitives;
import trufflesom.vm.NotYetImplementedException;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SSymbol;


public final class MessageSendNode {

  public static ExpressionNode create(final SSymbol selector,
      final ExpressionNode[] arguments, final long coord) {
    Specializer<ExpressionNode, SSymbol> specializer =
        Primitives.Current.getParserSpecializer(selector, arguments);
    if (specializer == null) {
      return new UninitializedMessageSendNode(selector, arguments).initialize(coord);
    }

    return specializer.create(null, arguments, coord);
  }

  private static final ExpressionNode[] NO_ARGS = new ExpressionNode[0];

  public static AbstractMessageSendNode createForPerformNodes(final SSymbol selector,
      final long coord) {
    return new UninitializedMessageSendNode(selector, NO_ARGS).initialize(coord);
  }

  public static GenericMessageSendNode createGeneric(final SSymbol selector,
      final ExpressionNode[] argumentNodes, final long coord) {
    return new GenericMessageSendNode(selector, argumentNodes,
        new UninitializedDispatchNode(selector)).initialize(coord);
  }

  public static AbstractMessageSendNode createSuperSend(final SClass superClass,
      final SSymbol selector, final ExpressionNode[] arguments, final long coord) {
    SInvokable method = superClass.lookupInvokable(selector);

    if (method == null) {
      throw new NotYetImplementedException(
          "Currently #dnu with super sent is not yet implemented. ");
    }

    if (method.isTrivial()) {
      PreevaluatedExpression node = method.copyTrivialNode();
      return new SuperExprNode(selector, arguments, node).initialize(coord);
    }

    DirectCallNode superMethodNode = Truffle.getRuntime().createDirectCallNode(
        method.getCallTarget());

    return new SuperSendNode(selector, arguments, superMethodNode).initialize(coord);
  }

  public static final class SuperSendNode extends AbstractMessageSendNode {
    private final SSymbol selector;
    private final int     numberOfSignatureArguments;

    @Child private DirectCallNode cachedSuperMethod;

    private SuperSendNode(final SSymbol selector, final ExpressionNode[] arguments,
        final DirectCallNode superMethod) {
      super(arguments);
      this.selector = selector;
      this.cachedSuperMethod = superMethod;
      this.numberOfSignatureArguments = selector.getNumberOfSignatureArguments();
    }

    @Override
    public Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return cachedSuperMethod.call(arguments);
    }

    @Override
    public SSymbol getInvocationIdentifier() {
      return selector;
    }

    @Override
    public String toString() {
      return "SuperSend(" + selector.getString() + ")";
    }

    @Override
    public int getNumberOfArguments() {
      return numberOfSignatureArguments;
    }
  }

  private static final class SuperExprNode extends AbstractMessageSendNode {
    private final SSymbol selector;
    private final int     numberOfSignatureArguments;

    @Child private PreevaluatedExpression expr;

    private SuperExprNode(final SSymbol selector, final ExpressionNode[] arguments,
        final PreevaluatedExpression expr) {
      super(arguments);
      this.selector = selector;
      this.expr = expr;
      this.numberOfSignatureArguments = selector.getNumberOfSignatureArguments();
    }

    @Override
    public Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return expr.doPreEvaluated(frame, arguments);
    }

    @Override
    public SSymbol getInvocationIdentifier() {
      return selector;
    }

    @Override
    public String toString() {
      return "SendExpr(" + selector.getString() + ")";
    }

    @Override
    public int getNumberOfArguments() {
      return numberOfSignatureArguments;
    }
  }
}
