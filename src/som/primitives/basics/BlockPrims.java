package som.primitives.basics;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.source.SourceSection;

import bd.primitives.Primitive;
import som.interpreter.nodes.dispatch.AbstractDispatchNode;
import som.interpreter.nodes.dispatch.UninitializedValuePrimDispatchNode;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.QuaternaryExpressionNode;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SBlock;


public abstract class BlockPrims {

  public interface ValuePrimitiveNode {
    void adoptNewDispatchListHead(AbstractDispatchNode node);
  }

  @GenerateNodeFactory
  @Primitive(className = "Block", primitive = "restart")
  public abstract static class RestartPrim extends UnaryExpressionNode {
    public RestartPrim(final SourceSection source) {
      super(source);
    }

    @Specialization
    public SAbstractObject doSBlock(final SBlock receiver) {
      CompilerDirectives.transferToInterpreter();
      // TruffleSOM intrinsifies #whileTrue: and #whileFalse:
      throw new RuntimeException("This primitive is not supported anymore! "
          + "Something went wrong with the intrinsification of "
          + "#whileTrue:/#whileFalse:?");
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Block", primitive = "value")
  @Primitive(className = "Block1", primitive = "value")
  @Primitive(selector = "value", inParser = false,
      receiverType = {SBlock.class, Boolean.class})
  public abstract static class ValueNonePrim extends UnaryExpressionNode
      implements ValuePrimitiveNode {
    @Child private AbstractDispatchNode dispatchNode;

    public ValueNonePrim(final SourceSection source) {
      super(source);
      dispatchNode = new UninitializedValuePrimDispatchNode();
    }

    @Specialization
    public final Object doSBlock(final VirtualFrame frame, final SBlock receiver) {
      return dispatchNode.executeDispatch(frame, new Object[] {receiver});
    }

    @Specialization
    public final boolean doBoolean(final boolean receiver) {
      return receiver;
    }

    @Override
    public final void adoptNewDispatchListHead(final AbstractDispatchNode node) {
      dispatchNode = insert(node);
    }

    @Override
    public NodeCost getCost() {
      int dispatchChain = dispatchNode.lengthOfDispatchChain();
      if (dispatchChain == 0) {
        return NodeCost.UNINITIALIZED;
      } else if (dispatchChain == 1) {
        return NodeCost.MONOMORPHIC;
      } else if (dispatchChain <= AbstractDispatchNode.INLINE_CACHE_SIZE) {
        return NodeCost.POLYMORPHIC;
      } else {
        return NodeCost.MEGAMORPHIC;
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Block2", primitive = "value:", selector = "value:", inParser = false,
      receiverType = SBlock.class)
  public abstract static class ValueOnePrim extends BinaryExpressionNode
      implements ValuePrimitiveNode {
    @Child private AbstractDispatchNode dispatchNode;

    public ValueOnePrim(final SourceSection source) {
      super(source);
      dispatchNode = new UninitializedValuePrimDispatchNode();
    }

    @Specialization
    public final Object doSBlock(final VirtualFrame frame, final SBlock receiver,
        final Object arg) {
      return dispatchNode.executeDispatch(frame, new Object[] {receiver, arg});
    }

    @Override
    public final void adoptNewDispatchListHead(final AbstractDispatchNode node) {
      dispatchNode = insert(node);
    }

    @Override
    public NodeCost getCost() {
      int dispatchChain = dispatchNode.lengthOfDispatchChain();
      if (dispatchChain == 0) {
        return NodeCost.UNINITIALIZED;
      } else if (dispatchChain == 1) {
        return NodeCost.MONOMORPHIC;
      } else if (dispatchChain <= AbstractDispatchNode.INLINE_CACHE_SIZE) {
        return NodeCost.POLYMORPHIC;
      } else {
        return NodeCost.MEGAMORPHIC;
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Block3", primitive = "value:with:", selector = "value:with:",
      inParser = false, receiverType = SBlock.class)
  public abstract static class ValueTwoPrim extends TernaryExpressionNode
      implements ValuePrimitiveNode {
    @Child private AbstractDispatchNode dispatchNode;

    public ValueTwoPrim(final SourceSection source) {
      super(source);
      dispatchNode = new UninitializedValuePrimDispatchNode();
    }

    @Specialization
    public final Object doSBlock(final VirtualFrame frame,
        final SBlock receiver, final Object arg1, final Object arg2) {
      return dispatchNode.executeDispatch(frame, new Object[] {receiver, arg1, arg2});
    }

    @Override
    public final void adoptNewDispatchListHead(final AbstractDispatchNode node) {
      dispatchNode = insert(node);
    }

    @Override
    public NodeCost getCost() {
      int dispatchChain = dispatchNode.lengthOfDispatchChain();
      if (dispatchChain == 0) {
        return NodeCost.UNINITIALIZED;
      } else if (dispatchChain == 1) {
        return NodeCost.MONOMORPHIC;
      } else if (dispatchChain <= AbstractDispatchNode.INLINE_CACHE_SIZE) {
        return NodeCost.POLYMORPHIC;
      } else {
        return NodeCost.MEGAMORPHIC;
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Block4", primitive = "value:with:with:")
  public abstract static class ValueMorePrim extends QuaternaryExpressionNode {
    public ValueMorePrim() {
      super(null);
    }

    @Specialization
    public final Object doSBlock(final VirtualFrame frame,
        final SBlock receiver, final Object firstArg, final Object secondArg,
        final Object thirdArg) {
      CompilerDirectives.transferToInterpreter();
      throw new RuntimeException(
          "This should never be called, because SOM Blocks have max. 2 arguments.");
    }
  }
}
