package som.interpreter.nodes;

import som.interpreter.TruffleCompiler;
import som.interpreter.TypesGen;
import som.interpreter.nodes.dispatch.AbstractDispatchNode;
import som.interpreter.nodes.dispatch.DispatchChain.Cost;
import som.interpreter.nodes.dispatch.GenericDispatchNode;
import som.interpreter.nodes.dispatch.SuperDispatchNode;
import som.interpreter.nodes.dispatch.UninitializedDispatchNode;
import som.interpreter.nodes.literals.BlockNode;
import som.interpreter.nodes.specialized.IfFalseMessageNodeFactory;
import som.interpreter.nodes.specialized.IfTrueIfFalseMessageNodeFactory;
import som.interpreter.nodes.specialized.IfTrueMessageNodeFactory;
import som.interpreter.nodes.specialized.IntDownToDoMessageNodeFactory;
import som.interpreter.nodes.specialized.IntToByDoMessageNodeFactory;
import som.interpreter.nodes.specialized.IntToDoMessageNodeFactory;
import som.interpreter.nodes.specialized.whileloops.WhileWithStaticBlocksNode.WhileFalseStaticBlocksNode;
import som.interpreter.nodes.specialized.whileloops.WhileWithStaticBlocksNode.WhileTrueStaticBlocksNode;
import som.vm.NotYetImplementedException;
import som.vmobjects.SBlock;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.source.SourceSection;


public final class MessageSendNode {

  public static AbstractMessageSendNode create(final SSymbol selector,
      final ExpressionNode[] arguments, final SourceSection source) {
    return new UninitializedMessageSendNode(selector, arguments, source);
  }

  public static AbstractMessageSendNode createForPerformNodes(final SSymbol selector) {
    return new UninitializedSymbolSendNode(selector, null);
  }

  public static GenericMessageSendNode createGeneric(final SSymbol selector,
      final ExpressionNode[] argumentNodes, final SourceSection source) {
    return new GenericMessageSendNode(selector, argumentNodes,
      new UninitializedDispatchNode(selector), source);
  }

  public abstract static class AbstractMessageSendNode extends ExpressionNode
      implements PreevaluatedExpression {

    @Children protected final ExpressionNode[] argumentNodes;

    protected AbstractMessageSendNode(final ExpressionNode[] arguments,
        final SourceSection source) {
      super(source);
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

    protected final SSymbol selector;

    protected AbstractUninitializedMessageSendNode(final SSymbol selector,
        final ExpressionNode[] arguments, final SourceSection source) {
      super(arguments, source);
      this.selector = selector;
    }

    @Override
    public final Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return specialize(arguments).
          doPreEvaluated(frame, arguments);
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
      // moment  we will leave it at this.
      // TODO: revisit, and also do more specific optimizations for super sends.


      // let's organize the specializations by number of arguments
      // perhaps not the best, but one simple way to just get some order into
      // the chaos.

      switch (argumentNodes.length) {
        case  1: return specializeUnary(arguments);
        case  2: return specializeBinary(arguments);
        case  3: return specializeTernary(arguments);
        case  4: return specializeQuaternary(arguments);
      }

      return makeGenericSend();
    }

    protected abstract PreevaluatedExpression makeSuperSend();

    private GenericMessageSendNode makeGenericSend() {
      GenericMessageSendNode send = new GenericMessageSendNode(selector,
          argumentNodes,
          new UninitializedDispatchNode(selector),
          getSourceSection());
      return replace(send);
    }

    protected PreevaluatedExpression specializeUnary(final Object[] args) {
      switch (selector.getString()) {
        // eagerly but cautious:
      }
      return makeGenericSend();
    }

    protected PreevaluatedExpression specializeBinary(final Object[] arguments) {
      switch (selector.getString()) {
        case "whileTrue:": {
          if (argumentNodes[1] instanceof BlockNode &&
              argumentNodes[0] instanceof BlockNode) {
            BlockNode argBlockNode = (BlockNode) argumentNodes[1];
            SBlock    argBlock     = (SBlock)    arguments[1];
            return replace(new WhileTrueStaticBlocksNode(
                (BlockNode) argumentNodes[0], argBlockNode,
                (SBlock) arguments[0],
                argBlock, getSourceSection()));
          }
          break; // use normal send
        }
        case "whileFalse:":
          if (argumentNodes[1] instanceof BlockNode &&
              argumentNodes[0] instanceof BlockNode) {
            BlockNode argBlockNode = (BlockNode) argumentNodes[1];
            SBlock    argBlock     = (SBlock)    arguments[1];
            return replace(new WhileFalseStaticBlocksNode(
                (BlockNode) argumentNodes[0], argBlockNode,
                (SBlock) arguments[0], argBlock, getSourceSection()));
          }
          break; // use normal send

        case "ifTrue:":
          return replace(IfTrueMessageNodeFactory.create(arguments[0],
              arguments[1], getSourceSection(),
              argumentNodes[0], argumentNodes[1]));
        case "ifFalse:":
          return replace(IfFalseMessageNodeFactory.create(arguments[0],
              arguments[1], getSourceSection(),
              argumentNodes[0], argumentNodes[1]));
      }

      return makeGenericSend();
    }

    protected PreevaluatedExpression specializeTernary(final Object[] arguments) {
      switch (selector.getString()) {
        case "ifTrue:ifFalse:":
          return replace(IfTrueIfFalseMessageNodeFactory.create(arguments[0],
              arguments[1], arguments[2], argumentNodes[0],
              argumentNodes[1], argumentNodes[2]));
        case "to:do:":
          if (TypesGen.TYPES.isLong(arguments[0]) &&
              (TypesGen.TYPES.isLong(arguments[1]) ||
                  TypesGen.TYPES.isDouble(arguments[1])) &&
              TypesGen.TYPES.isSBlock(arguments[2])) {
            return replace(IntToDoMessageNodeFactory.create(this,
                (SBlock) arguments[2], argumentNodes[0], argumentNodes[1],
                argumentNodes[2]));
          }
          break;
        case "downTo:do:":
          if (TypesGen.TYPES.isLong(arguments[0]) &&
              (TypesGen.TYPES.isLong(arguments[1]) ||
                  TypesGen.TYPES.isDouble(arguments[1])) &&
              TypesGen.TYPES.isSBlock(arguments[2])) {
            return replace(IntDownToDoMessageNodeFactory.create(this,
                (SBlock) arguments[2], argumentNodes[0], argumentNodes[1],
                argumentNodes[2]));
          }
          break;
      }
      return makeGenericSend();
    }

    protected PreevaluatedExpression specializeQuaternary(
        final Object[] arguments) {
      switch (selector.getString()) {
        case "to:by:do:":
          return replace(IntToByDoMessageNodeFactory.create(this,
              (SBlock) arguments[3], argumentNodes[0], argumentNodes[1],
              argumentNodes[2], argumentNodes[3]));
      }
      return makeGenericSend();
    }
  }

  private static final class UninitializedMessageSendNode
      extends AbstractUninitializedMessageSendNode {

    protected UninitializedMessageSendNode(final SSymbol selector,
        final ExpressionNode[] arguments, final SourceSection source) {
      super(selector, arguments, source);
    }

    @Override
    protected PreevaluatedExpression makeSuperSend() {
      GenericMessageSendNode node = new GenericMessageSendNode(selector,
        argumentNodes, SuperDispatchNode.create(selector,
              (ISuperReadNode) argumentNodes[0]), getSourceSection());
      return replace(node);
    }
  }

  private static final class UninitializedSymbolSendNode
    extends AbstractUninitializedMessageSendNode {

    protected UninitializedSymbolSendNode(final SSymbol selector,
        final SourceSection source) {
      super(selector, new ExpressionNode[0], source);
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

    @Override
    protected PreevaluatedExpression specializeBinary(final Object[] arguments) {
      switch (selector.getString()) {
      }

      return super.specializeBinary(arguments);
    }
  }

/// TODO: currently, we do not only specialize the given stuff above, but also what has been classified as 'value' sends in the OMOP branch. Is that a problem?

  public static final class GenericMessageSendNode
      extends AbstractMessageSendNode {

    private final SSymbol selector;

    @Child private AbstractDispatchNode dispatchNode;

    private GenericMessageSendNode(final SSymbol selector,
        final ExpressionNode[] arguments,
        final AbstractDispatchNode dispatchNode, final SourceSection source) {
      super(arguments, source);
      this.selector = selector;
      this.dispatchNode = dispatchNode;
    }

    @Override
    public Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return dispatchNode.executeDispatch(frame, arguments);
    }

    public AbstractDispatchNode getDispatchListHead() {
      return dispatchNode;
    }

    public void adoptNewDispatchListHead(final AbstractDispatchNode newHead) {
      CompilerAsserts.neverPartOfCompilation();
      dispatchNode = insert(newHead);
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
  }
}
