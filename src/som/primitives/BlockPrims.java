package som.primitives;

import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.QuaternaryExpressionNode;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SBlock;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;


public abstract class BlockPrims {

  public abstract static class RestartPrim extends UnaryExpressionNode {
    public RestartPrim() { super(null); }

    @Specialization
    public SAbstractObject doSBlock(final SBlock receiver) {
      CompilerDirectives.transferToInterpreter();
      // TruffleSOM intrinsifies #whileTrue: and #whileFalse:
      throw new RuntimeException("This primitive is not supported anymore! "
          + "Something went wrong with the intrinsification of "
          + "#whileTrue:/#whileFalse:?");
    }
  }

  public abstract static class ValueNonePrim extends UnaryExpressionNode {
    @Child private IndirectCallNode call;

    public ValueNonePrim() {
      super(null);
      call = Truffle.getRuntime().createIndirectCallNode();
    }

    @Specialization
    public final Object doSBlock(final VirtualFrame frame, final SBlock receiver) {
      return call.call(frame, receiver.getMethod().getCallTarget(),
          new Object[] {receiver});
    }

    @Specialization
    public final boolean doBoolean(final boolean receiver) {
      return receiver;
    }
  }

  public abstract static class ValueOnePrim extends BinaryExpressionNode {
    @Child private IndirectCallNode call;

    public ValueOnePrim() {
      super(null);
      call = Truffle.getRuntime().createIndirectCallNode();
    }

    @Specialization
    public final Object doSBlock(final VirtualFrame frame, final SBlock receiver,
        final Object arg) {
      return call.call(frame, receiver.getMethod().getCallTarget(),
          new Object[] {receiver, arg});
    }
  }

  public abstract static class ValueTwoPrim extends TernaryExpressionNode {
    @Child private IndirectCallNode call;

    public ValueTwoPrim() {
      super(null);
      call = Truffle.getRuntime().createIndirectCallNode();
    }

    @Specialization
    public final Object doSBlock(final VirtualFrame frame,
        final SBlock receiver, final Object arg1, final Object arg2) {
      return call.call(frame, receiver.getMethod().getCallTarget(),
          new Object[] {receiver, arg1, arg2});
    }
  }

  public abstract static class ValueMorePrim extends QuaternaryExpressionNode {
    public ValueMorePrim() { super(null); }
    @Specialization
    public final Object doSBlock(final VirtualFrame frame,
        final SBlock receiver, final Object firstArg, final Object secondArg,
        final Object thirdArg) {
      CompilerDirectives.transferToInterpreter();
      throw new RuntimeException("This should never be called, because SOM Blocks have max. 2 arguments.");
    }
  }
}
