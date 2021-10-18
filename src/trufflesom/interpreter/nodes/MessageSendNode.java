package trufflesom.interpreter.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.source.SourceSection;

import bd.primitives.Specializer;
import bd.primitives.nodes.PreevaluatedExpression;
import bd.tools.nodes.Invocation;
import trufflesom.interpreter.TruffleCompiler;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.DispatchChain.Cost;
import trufflesom.interpreter.nodes.dispatch.GenericDispatchNode;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.interpreter.nodes.nary.EagerlySpecializableNode;
import trufflesom.primitives.Primitives;
import trufflesom.vm.NotYetImplementedException;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SSymbol;


public final class MessageSendNode {

  public static ExpressionNode create(final SSymbol selector,
      final ExpressionNode[] arguments, final SourceSection source, final Universe universe) {
    Primitives prims = universe.getPrimitives();
    Specializer<Universe, ExpressionNode, SSymbol> specializer =
        prims.getParserSpecializer(selector, arguments);
    if (specializer == null) {
      return new UninitializedMessageSendNode(
          selector, arguments, universe).initialize(source);
    }

    EagerlySpecializableNode newNode = (EagerlySpecializableNode) specializer.create(null,
        arguments, source, !specializer.noWrapper(), universe);

    if (specializer.noWrapper()) {
      return newNode;
    } else {
      return newNode.wrapInEagerWrapper(selector, arguments, universe);
    }
  }

  public static AbstractMessageSendNode createForPerformNodes(final SSymbol selector,
      final SourceSection source, final Universe universe) {
    return new UninitializedSymbolSendNode(selector, universe).initialize(source);
  }

  public static AbstractMessageSendNode createGeneric(final SSymbol selector,
      final ExpressionNode[] argumentNodes, final SourceSection source,
      final Universe universe) {
    UninitializedDispatchNode uninit = new UninitializedDispatchNode(selector, universe);

    if (argumentNodes.length == 1) {
      return new UnaryMessageSendNode(selector, argumentNodes[0], uninit).initialize(source);
    }

    if (argumentNodes.length == 2) {
      return new BinaryMessageSendNode(selector, argumentNodes[0], argumentNodes[1],
          uninit).initialize(source);
    }

    if (argumentNodes.length == 3) {
      return new TernaryMessageSendNode(selector, argumentNodes[0], argumentNodes[1],
          argumentNodes[2], uninit).initialize(source);
    }

    if (argumentNodes.length == 4) {
      return new QuaternaryMessageSendNode(selector, argumentNodes[0], argumentNodes[1],
          argumentNodes[2], argumentNodes[3], uninit).initialize(source);
    }

    return new GenericMessageSendNode(selector, argumentNodes, uninit).initialize(source);
  }

  public static AbstractMessageSendNode createSuperSend(final SClass superClass,
      final SSymbol selector, final ExpressionNode[] arguments, final SourceSection source) {
    SInvokable method = superClass.lookupInvokable(selector);

    if (method == null) {
      throw new NotYetImplementedException(
          "Currently #dnu with super sent is not yet implemented. ");
    }

    if (method.isTrivial()) {
      PreevaluatedExpression node = method.copyTrivialNode();
      return new SuperExprNode(selector, arguments, node).initialize(source);
    }

    DirectCallNode superMethodNode = Truffle.getRuntime().createDirectCallNode(
        method.getCallTarget());

    if (arguments.length == 1) {
      return new UnarySuperSendNode(selector, arguments[0], superMethodNode).initialize(
          source);
    }
    if (arguments.length == 2) {
      return new BinarySuperSendNode(selector, arguments[0], arguments[1],
          superMethodNode).initialize(source);
    }
    if (arguments.length == 3) {
      return new TernarySuperSendNode(selector, arguments[0], arguments[1], arguments[2],
          superMethodNode).initialize(source);
    }
    if (arguments.length == 4) {
      return new QuaternarySuperSendNode(selector, arguments[0], arguments[1], arguments[2],
          arguments[3], superMethodNode).initialize(source);
    }

    return new SuperSendNode(selector, arguments, superMethodNode).initialize(source);
  }

  public abstract static class AbstractMessageSendNode extends ExpressionNode
      implements PreevaluatedExpression, Invocation<SSymbol> {
    public abstract void replaceDispatchListHead(GenericDispatchNode replacement);
  }

  public abstract static class AbstractNaryMessageSendNode extends AbstractMessageSendNode {
    @Children protected final ExpressionNode[] argumentNodes;

    protected AbstractNaryMessageSendNode(final ExpressionNode[] arguments) {
      this.argumentNodes = arguments;
    }

    @Override
    public final Object executeGeneric(final VirtualFrame frame) {
      Object[] arguments = evaluateArguments(frame);
      return doPreEvaluated(frame, arguments);
    }

    @ExplodeLoop
    private Object[] evaluateArguments(final VirtualFrame frame) {
      Object[] arguments = new Object[argumentNodes.length];
      for (int i = 0; i < argumentNodes.length; i++) {
        arguments[i] = argumentNodes[i].executeGeneric(frame);
        assert arguments[i] != null;
      }
      return arguments;
    }
  }

  public abstract static class AbstractUninitializedMessageSendNode
      extends AbstractNaryMessageSendNode {

    protected final SSymbol  selector;
    protected final Universe universe;

    protected AbstractUninitializedMessageSendNode(final SSymbol selector,
        final ExpressionNode[] arguments, final Universe universe) {
      super(arguments);
      this.selector = selector;
      this.universe = universe;
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "(" + selector.getString() + ")";
    }

    @Override
    public final Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return specialize(arguments).doPreEvaluated(frame, arguments);
    }

    @Override
    public final void replaceDispatchListHead(
        final GenericDispatchNode replacement) {
      throw new UnsupportedOperationException();
    }

    private PreevaluatedExpression specialize(final Object[] arguments) {
      TruffleCompiler.transferToInterpreterAndInvalidate("Specialize Message Node");

      // We treat super sends separately for simplicity, might not be the
      // optimal solution, especially in cases were the knowledge of the
      // receiver class also allows us to do more specific things, but for the
      // moment we will leave it at this.
      // TODO: revisit, and also do more specific optimizations for super sends.

      Primitives prims = universe.getPrimitives();

      Specializer<Universe, ExpressionNode, SSymbol> specializer =
          prims.getEagerSpecializer(selector, arguments, argumentNodes);

      if (specializer != null) {
        boolean noWrapper = specializer.noWrapper();
        EagerlySpecializableNode newNode =
            (EagerlySpecializableNode) specializer.create(arguments, argumentNodes,
                sourceSection, !noWrapper, universe);
        if (noWrapper) {
          return replace(newNode);
        } else {
          return makeEagerPrim(newNode);
        }
      }

      return makeGenericSend(arguments.length);
    }

    private PreevaluatedExpression makeEagerPrim(final EagerlySpecializableNode prim) {
      assert prim.getSourceSection() != null;

      PreevaluatedExpression result = (PreevaluatedExpression) replace(
          prim.wrapInEagerWrapper(selector, argumentNodes, universe));

      return result;
    }

    private AbstractMessageSendNode makeGenericSend(final int numArgs) {
      ExpressionNode[] argumentNodes =
          (this.argumentNodes.length == 0) ? new ExpressionNode[numArgs] : this.argumentNodes;
      if (numArgs == 1) {
        UnaryMessageSendNode send =
            new UnaryMessageSendNode(selector, argumentNodes[0],
                new UninitializedDispatchNode(selector, universe)).initialize(sourceSection);
        return replace(send);
      }

      if (numArgs == 2) {
        BinaryMessageSendNode send =
            new BinaryMessageSendNode(selector, argumentNodes[0], argumentNodes[1],
                new UninitializedDispatchNode(selector, universe)).initialize(sourceSection);
        return replace(send);
      }

      if (numArgs == 3) {
        TernaryMessageSendNode send =
            new TernaryMessageSendNode(selector, argumentNodes[0], argumentNodes[1],
                argumentNodes[2],
                new UninitializedDispatchNode(selector, universe)).initialize(sourceSection);
        return replace(send);
      }

      if (numArgs == 4) {
        QuaternaryMessageSendNode send =
            new QuaternaryMessageSendNode(selector, argumentNodes[0], argumentNodes[1],
                argumentNodes[2], argumentNodes[3],
                new UninitializedDispatchNode(selector, universe)).initialize(sourceSection);
        return replace(send);
      }

      GenericMessageSendNode send = new GenericMessageSendNode(selector, argumentNodes,
          new UninitializedDispatchNode(selector, universe)).initialize(sourceSection);
      return replace(send);
    }

    @Override
    public SSymbol getInvocationIdentifier() {
      return selector;
    }

  }

  private static final class UninitializedMessageSendNode
      extends AbstractUninitializedMessageSendNode {

    protected UninitializedMessageSendNode(final SSymbol selector,
        final ExpressionNode[] arguments, final Universe universe) {
      super(selector, arguments, universe);
    }
  }

  private static final class UninitializedSymbolSendNode
      extends AbstractUninitializedMessageSendNode {

    protected UninitializedSymbolSendNode(final SSymbol selector, final Universe universe) {
      super(selector, new ExpressionNode[0], universe);
    }
  }

  // TODO: currently, we do not only specialize the given stuff above, but also what has been
  // classified as 'value' sends in the OMOP branch. Is that a problem?

  public static final class GenericMessageSendNode
      extends AbstractNaryMessageSendNode {

    private final SSymbol selector;

    @Child private AbstractDispatchNode dispatchNode;

    private GenericMessageSendNode(final SSymbol selector, final ExpressionNode[] arguments,
        final AbstractDispatchNode dispatchNode) {
      super(arguments);
      assert arguments.length > 4 : "Use Unary, Binary... variants instead";
      this.selector = selector;
      this.dispatchNode = dispatchNode;
    }

    @Override
    public Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return dispatchNode.executeDispatch(frame, arguments);
    }

    @Override
    public void replaceDispatchListHead(
        final GenericDispatchNode replacement) {
      CompilerAsserts.neverPartOfCompilation();
      dispatchNode.replace(replacement);
    }

    @Override
    public String toString() {
      return "GMsgSend(" + selector.getString() + ")";
    }

    @Override
    public NodeCost getCost() {
      return Cost.getCost(dispatchNode);
    }

    @Override
    public SSymbol getInvocationIdentifier() {
      return selector;
    }
  }

  public static final class UnaryMessageSendNode extends AbstractMessageSendNode {
    @Child private ExpressionNode       rcvr;
    @Child private AbstractDispatchNode dispatchNode;

    private final SSymbol selector;

    private UnaryMessageSendNode(final SSymbol selector, final ExpressionNode rcvr,
        final AbstractDispatchNode dispatchNode) {
      this.rcvr = rcvr;
      this.dispatchNode = dispatchNode;
      this.selector = selector;
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      Object rcvr = this.rcvr.executeGeneric(frame);
      return dispatchNode.executeUnary(frame, rcvr);
    }

    @Override
    public Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return dispatchNode.executeUnary(frame, arguments[0]);
    }

    @Override
    public void replaceDispatchListHead(
        final GenericDispatchNode replacement) {
      CompilerAsserts.neverPartOfCompilation();
      dispatchNode.replace(replacement);
    }

    @Override
    public String toString() {
      return "GMsgSend(" + selector.getString() + ")";
    }

    @Override
    public NodeCost getCost() {
      return Cost.getCost(dispatchNode);
    }

    @Override
    public SSymbol getInvocationIdentifier() {
      return selector;
    }
  }

  public static final class BinaryMessageSendNode extends AbstractMessageSendNode {
    @Child private ExpressionNode       rcvr;
    @Child private ExpressionNode       arg;
    @Child private AbstractDispatchNode dispatchNode;

    private final SSymbol selector;

    private BinaryMessageSendNode(final SSymbol selector, final ExpressionNode rcvr,
        final ExpressionNode arg, final AbstractDispatchNode dispatchNode) {
      this.rcvr = rcvr;
      this.arg = arg;
      this.dispatchNode = dispatchNode;
      this.selector = selector;
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      Object rcvr = this.rcvr.executeGeneric(frame);
      Object arg = this.arg.executeGeneric(frame);

      return dispatchNode.executeBinary(frame, rcvr, arg);
    }

    @Override
    public Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return dispatchNode.executeBinary(frame, arguments[0], arguments[1]);
    }

    @Override
    public void replaceDispatchListHead(
        final GenericDispatchNode replacement) {
      CompilerAsserts.neverPartOfCompilation();
      dispatchNode.replace(replacement);
    }

    @Override
    public String toString() {
      return "GMsgSend(" + selector.getString() + ")";
    }

    @Override
    public NodeCost getCost() {
      return Cost.getCost(dispatchNode);
    }

    @Override
    public SSymbol getInvocationIdentifier() {
      return selector;
    }
  }

  public static final class TernaryMessageSendNode extends AbstractMessageSendNode {
    @Child private ExpressionNode       rcvr;
    @Child private ExpressionNode       arg1;
    @Child private ExpressionNode       arg2;
    @Child private AbstractDispatchNode dispatchNode;

    private final SSymbol selector;

    private TernaryMessageSendNode(final SSymbol selector, final ExpressionNode rcvr,
        final ExpressionNode arg1, final ExpressionNode arg2,
        final AbstractDispatchNode dispatchNode) {
      this.rcvr = rcvr;
      this.arg1 = arg1;
      this.arg2 = arg2;
      this.dispatchNode = dispatchNode;
      this.selector = selector;
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      Object rcvr = this.rcvr.executeGeneric(frame);
      Object arg1 = this.arg1.executeGeneric(frame);
      Object arg2 = this.arg2.executeGeneric(frame);

      return dispatchNode.executeTernary(frame, rcvr, arg1, arg2);
    }

    @Override
    public Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return dispatchNode.executeTernary(frame, arguments[0], arguments[1], arguments[2]);
    }

    @Override
    public void replaceDispatchListHead(
        final GenericDispatchNode replacement) {
      CompilerAsserts.neverPartOfCompilation();
      dispatchNode.replace(replacement);
    }

    @Override
    public String toString() {
      return "GMsgSend(" + selector.getString() + ")";
    }

    @Override
    public NodeCost getCost() {
      return Cost.getCost(dispatchNode);
    }

    @Override
    public SSymbol getInvocationIdentifier() {
      return selector;
    }
  }

  public static final class QuaternaryMessageSendNode extends AbstractMessageSendNode {
    @Child private ExpressionNode       rcvr;
    @Child private ExpressionNode       arg1;
    @Child private ExpressionNode       arg2;
    @Child private ExpressionNode       arg3;
    @Child private AbstractDispatchNode dispatchNode;

    private final SSymbol selector;

    private QuaternaryMessageSendNode(final SSymbol selector, final ExpressionNode rcvr,
        final ExpressionNode arg1, final ExpressionNode arg2, final ExpressionNode arg3,
        final AbstractDispatchNode dispatchNode) {
      this.rcvr = rcvr;
      this.arg1 = arg1;
      this.arg2 = arg2;
      this.arg3 = arg3;
      this.dispatchNode = dispatchNode;
      this.selector = selector;
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      Object rcvr = this.rcvr.executeGeneric(frame);
      Object arg1 = this.arg1.executeGeneric(frame);
      Object arg2 = this.arg2.executeGeneric(frame);
      Object arg3 = this.arg3.executeGeneric(frame);

      return dispatchNode.executeQuat(frame, rcvr, arg1, arg2, arg3);
    }

    @Override
    public Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return dispatchNode.executeQuat(frame, arguments[0], arguments[1], arguments[2],
          arguments[3]);
    }

    @Override
    public void replaceDispatchListHead(
        final GenericDispatchNode replacement) {
      CompilerAsserts.neverPartOfCompilation();
      dispatchNode.replace(replacement);
    }

    @Override
    public String toString() {
      return "GMsgSend(" + selector.getString() + ")";
    }

    @Override
    public NodeCost getCost() {
      return Cost.getCost(dispatchNode);
    }

    @Override
    public SSymbol getInvocationIdentifier() {
      return selector;
    }
  }

  public static final class SuperSendNode extends AbstractNaryMessageSendNode {
    private final SSymbol selector;

    @Child private DirectCallNode cachedSuperMethod;

    private SuperSendNode(final SSymbol selector, final ExpressionNode[] arguments,
        final DirectCallNode superMethod) {
      super(arguments);
      assert arguments.length > 4;
      this.selector = selector;
      this.cachedSuperMethod = superMethod;
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
    public void replaceDispatchListHead(
        final GenericDispatchNode replacement) {
      throw new UnsupportedOperationException();
    }
  }

  public static final class UnarySuperSendNode extends AbstractMessageSendNode {
    @Child private ExpressionNode rcvr;
    private final SSymbol         selector;

    @Child private DirectCallNode cachedSuperMethod;

    private UnarySuperSendNode(final SSymbol selector, final ExpressionNode rcvr,
        final DirectCallNode superMethod) {
      this.rcvr = rcvr;
      this.selector = selector;
      this.cachedSuperMethod = superMethod;
    }

    @Override
    public Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return cachedSuperMethod.call1(arguments[0]);
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      Object rcvr = this.rcvr.executeGeneric(frame);
      return cachedSuperMethod.call1(rcvr);
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
    public void replaceDispatchListHead(
        final GenericDispatchNode replacement) {
      throw new UnsupportedOperationException();
    }
  }

  public static final class BinarySuperSendNode extends AbstractMessageSendNode {
    @Child private ExpressionNode rcvr;
    @Child private ExpressionNode arg1;
    private final SSymbol         selector;

    @Child private DirectCallNode cachedSuperMethod;

    private BinarySuperSendNode(final SSymbol selector, final ExpressionNode rcvr,
        final ExpressionNode arg1, final DirectCallNode superMethod) {
      this.rcvr = rcvr;
      this.arg1 = arg1;
      this.selector = selector;
      this.cachedSuperMethod = superMethod;
    }

    @Override
    public Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return cachedSuperMethod.call2(arguments[0], arguments[1]);
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      Object rcvr = this.rcvr.executeGeneric(frame);
      Object arg1 = this.arg1.executeGeneric(frame);
      return cachedSuperMethod.call2(rcvr, arg1);
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
    public void replaceDispatchListHead(
        final GenericDispatchNode replacement) {
      throw new UnsupportedOperationException();
    }
  }

  public static final class TernarySuperSendNode extends AbstractMessageSendNode {
    @Child private ExpressionNode rcvr;
    @Child private ExpressionNode arg1;
    @Child private ExpressionNode arg2;
    private final SSymbol         selector;

    @Child private DirectCallNode cachedSuperMethod;

    private TernarySuperSendNode(final SSymbol selector, final ExpressionNode rcvr,
        final ExpressionNode arg1, final ExpressionNode arg2,
        final DirectCallNode superMethod) {
      this.rcvr = rcvr;
      this.arg1 = arg1;
      this.arg2 = arg2;
      this.selector = selector;
      this.cachedSuperMethod = superMethod;
    }

    @Override
    public Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return cachedSuperMethod.call3(arguments[0], arguments[1], arguments[2]);
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      Object rcvr = this.rcvr.executeGeneric(frame);
      Object arg1 = this.arg1.executeGeneric(frame);
      Object arg2 = this.arg2.executeGeneric(frame);
      return cachedSuperMethod.call3(rcvr, arg1, arg2);
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
    public void replaceDispatchListHead(
        final GenericDispatchNode replacement) {
      throw new UnsupportedOperationException();
    }
  }

  public static final class QuaternarySuperSendNode extends AbstractMessageSendNode {
    @Child private ExpressionNode rcvr;
    @Child private ExpressionNode arg1;
    @Child private ExpressionNode arg2;
    @Child private ExpressionNode arg3;
    private final SSymbol         selector;

    @Child private DirectCallNode cachedSuperMethod;

    private QuaternarySuperSendNode(final SSymbol selector, final ExpressionNode rcvr,
        final ExpressionNode arg1, final ExpressionNode arg2, final ExpressionNode arg3,
        final DirectCallNode superMethod) {
      this.rcvr = rcvr;
      this.arg1 = arg1;
      this.arg2 = arg2;
      this.arg3 = arg3;
      this.selector = selector;
      this.cachedSuperMethod = superMethod;
    }

    @Override
    public Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return cachedSuperMethod.call4(arguments[0], arguments[1], arguments[2], arguments[3]);
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      Object rcvr = this.rcvr.executeGeneric(frame);
      Object arg1 = this.arg1.executeGeneric(frame);
      Object arg2 = this.arg2.executeGeneric(frame);
      Object arg3 = this.arg3.executeGeneric(frame);
      return cachedSuperMethod.call4(rcvr, arg1, arg2, arg3);
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
    public void replaceDispatchListHead(
        final GenericDispatchNode replacement) {
      throw new UnsupportedOperationException();
    }
  }

  private static final class SuperExprNode extends AbstractNaryMessageSendNode {
    private final SSymbol                 selector;
    @Child private PreevaluatedExpression expr;

    private SuperExprNode(final SSymbol selector, final ExpressionNode[] arguments,
        final PreevaluatedExpression expr) {
      super(arguments);
      this.selector = selector;
      this.expr = expr;
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
    public void replaceDispatchListHead(
        final GenericDispatchNode replacement) {
      throw new UnsupportedOperationException();
    }
  }
}
