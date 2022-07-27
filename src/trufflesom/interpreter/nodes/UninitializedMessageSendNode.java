package trufflesom.interpreter.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;

import bdt.primitives.Specializer;
import bdt.primitives.nodes.PreevaluatedExpression;
import trufflesom.interpreter.nodes.dispatch.GenericDispatchNode;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.primitives.Primitives;
import trufflesom.vmobjects.SSymbol;


public final class UninitializedMessageSendNode extends AbstractMessageSendNode {

  protected final SSymbol selector;

  protected UninitializedMessageSendNode(final SSymbol selector,
      final ExpressionNode[] arguments) {
    super(selector.getNumberOfSignatureArguments(), arguments);
    this.selector = selector;
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
    CompilerDirectives.transferToInterpreterAndInvalidate();

    // We treat super sends separately for simplicity, might not be the
    // optimal solution, especially in cases were the knowledge of the
    // receiver class also allows us to do more specific things, but for the
    // moment we will leave it at this.
    // TODO: revisit, and also do more specific optimizations for super sends.
    Specializer<ExpressionNode, SSymbol> specializer =
        Primitives.Current.getEagerSpecializer(selector, arguments, argumentNodes);

    if (specializer != null) {
      PreevaluatedExpression newNode =
          specializer.create(arguments, argumentNodes, sourceCoord);

      replace((ExpressionNode) newNode);
      // I don't think I get to the inserted nodes...
      // notifyInserted((ExpressionNode) newNode);
      return newNode;
    }

    return makeGenericSend();
  }

  private AbstractMessageSendNode makeGenericSend() {
    int numArgs = argumentNodes.length;
    AbstractMessageSendNode send;
    if (numArgs == 1) {
      send = MessageSendNode.createGenericUnary(selector, argumentNodes[0], sourceCoord);
    } else if (numArgs == 2) {
      send = MessageSendNode.createGenericBinary(selector, argumentNodes[0], argumentNodes[1],
          sourceCoord);
    } else if (numArgs == 3) {
      send = MessageSendNode.createGenericTernary(selector, argumentNodes[0], argumentNodes[1],
          argumentNodes[2], sourceCoord);
    } else if (numArgs == 4) {
      send = MessageSendNode.createGenericQuat(selector, argumentNodes[0], argumentNodes[1],
          argumentNodes[2], argumentNodes[3], sourceCoord);
    } else {
      send = new GenericMessageSendNode(selector, argumentNodes,
          new UninitializedDispatchNode(selector)).initialize(sourceCoord);
    }
    replace(send);
    send.notifyDispatchInserted();
    return send;
  }

  @Override
  public SSymbol getInvocationIdentifier() {
    return selector;
  }

  @Override
  public void replaceDispatchListHead(final GenericDispatchNode replacement) {
    CompilerDirectives.transferToInterpreter();
    throw new UnsupportedOperationException();
  }

  @Override
  public void notifyDispatchInserted() {
    throw new UnsupportedOperationException();
  }
}
