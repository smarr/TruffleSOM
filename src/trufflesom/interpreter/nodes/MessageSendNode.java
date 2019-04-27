package trufflesom.interpreter.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.source.SourceSection;

import bd.primitives.Specializer;
import bd.tools.nodes.Invocation;
import trufflesom.interpreter.TruffleCompiler;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.DispatchChain.Cost;
import trufflesom.interpreter.nodes.dispatch.GenericDispatchNode;
import trufflesom.interpreter.nodes.dispatch.SuperDispatchNode;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.interpreter.nodes.nary.EagerlySpecializableNode;
import trufflesom.primitives.Primitives;
import trufflesom.vm.NotYetImplementedException;
import trufflesom.vm.Universe;
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

  public static GenericMessageSendNode createGeneric(final SSymbol selector,
      final ExpressionNode[] argumentNodes, final SourceSection source,
      final Universe universe) {
    return new GenericMessageSendNode(selector, argumentNodes,
        new UninitializedDispatchNode(selector, universe)).initialize(source);
  }

  public abstract static class AbstractMessageSendNode extends ExpressionNode
      implements PreevaluatedExpression, Invocation<SSymbol> {

    @Children protected final ExpressionNode[] argumentNodes;

    protected AbstractMessageSendNode(final ExpressionNode[] arguments) {
      this.argumentNodes = arguments;
    }

    public boolean isSuperSend() {
      return argumentNodes[0] instanceof ISuperReadNode;
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
      extends AbstractMessageSendNode {

    protected final SSymbol  selector;
    protected final Universe universe;

    protected AbstractUninitializedMessageSendNode(final SSymbol selector,
        final ExpressionNode[] arguments, final Universe universe) {
      super(arguments);
      this.selector = selector;
      this.universe = universe;
    }

    @Override
    public final Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return specialize(arguments).doPreEvaluated(frame, arguments);
    }

    private PreevaluatedExpression specialize(final Object[] arguments) {
      TruffleCompiler.transferToInterpreterAndInvalidate("Specialize Message Node");

      // first option is a super send, super sends are treated specially because
      // the receiver class is lexically determined
      if (isSuperSend()) {
        return makeSuperSend();
      }

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

      return makeGenericSend();
    }

    private PreevaluatedExpression makeEagerPrim(final EagerlySpecializableNode prim) {
      assert prim.getSourceSection() != null;

      PreevaluatedExpression result = (PreevaluatedExpression) replace(
          prim.wrapInEagerWrapper(selector, argumentNodes, universe));

      return result;
    }

    protected abstract PreevaluatedExpression makeSuperSend();

    private GenericMessageSendNode makeGenericSend() {
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

    @Override
    protected PreevaluatedExpression makeSuperSend() {
      GenericMessageSendNode node = new GenericMessageSendNode(selector, argumentNodes,
          SuperDispatchNode.create(selector, (ISuperReadNode) argumentNodes[0],
              universe)).initialize(sourceSection);
      return replace(node);
    }
  }

  private static final class UninitializedSymbolSendNode
      extends AbstractUninitializedMessageSendNode {

    protected UninitializedSymbolSendNode(final SSymbol selector, final Universe universe) {
      super(selector, new ExpressionNode[0], universe);
    }

    @Override
    public boolean isSuperSend() {
      // TODO: is is correct?
      return false;
    }

    @Override
    protected PreevaluatedExpression makeSuperSend() {
      // should never be reached with isSuperSend() returning always false
      throw new NotYetImplementedException();
    }
  }

  // TODO: currently, we do not only specialize the given stuff above, but also what has been
  // classified as 'value' sends in the OMOP branch. Is that a problem?

  public static final class GenericMessageSendNode
      extends AbstractMessageSendNode {

    private final SSymbol selector;

    @Child private AbstractDispatchNode dispatchNode;

    private GenericMessageSendNode(final SSymbol selector, final ExpressionNode[] arguments,
        final AbstractDispatchNode dispatchNode) {
      super(arguments);
      this.selector = selector;
      this.dispatchNode = dispatchNode;
    }

    @Override
    public Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return dispatchNode.executeDispatch(frame, arguments);
    }

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
}
