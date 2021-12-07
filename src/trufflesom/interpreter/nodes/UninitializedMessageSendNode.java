package trufflesom.interpreter.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import bd.primitives.Specializer;
import bd.primitives.nodes.PreevaluatedExpression;
import trufflesom.interpreter.TruffleCompiler;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.primitives.Primitives;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SSymbol;


public final class UninitializedMessageSendNode extends AbstractMessageSendNode {

  protected final SSymbol  selector;
  protected final Universe universe;

  protected UninitializedMessageSendNode(final SSymbol selector,
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
  public Object doPreEvaluated(final VirtualFrame frame, final Object[] arguments) {
    return specialize(arguments).doPreEvaluated(frame, arguments);
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
      PreevaluatedExpression newNode =
          (PreevaluatedExpression) specializer.create(arguments, argumentNodes,
              sourceSection, universe);

      return (PreevaluatedExpression) replace((ExpressionNode) newNode);
    }

    return makeGenericSend();
  }

  private GenericMessageSendNode makeGenericSend() {
    GenericMessageSendNode send = new GenericMessageSendNode(selector, argumentNodes,
        new UninitializedDispatchNode(selector, universe)).initialize(sourceSection);
    return replace(send);
  }

  @Override
  public SSymbol getInvocationIdentifier() {
    return selector;
  }

  @Override
  public int getNumberOfArguments() {
    return selector.getNumberOfSignatureArguments();
  }
}
