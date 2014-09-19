package som.primitives.reflection;

import som.interpreter.SArguments;
import som.interpreter.Types;
import som.interpreter.nodes.dispatch.DispatchChain;
import som.primitives.arrays.ToArgumentsArrayNode;
import som.primitives.arrays.ToArgumentsArrayNodeFactory;
import som.vmobjects.SArray;
import som.vmobjects.SInvokable;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;


public abstract class AbstractSymbolDispatch extends Node implements DispatchChain {
  public static final int INLINE_CACHE_SIZE = 6;

  public static AbstractSymbolDispatch create() {
    return new GenericDispatchNode();
  }

  protected final int depth;

  public AbstractSymbolDispatch(final int depth) {
    this.depth = depth;
  }

  public abstract Object executeDispatch(VirtualFrame frame, Object receiver,
      SSymbol selector, SArray argsArr);

  private static final class GenericDispatchNode extends AbstractSymbolDispatch {

    @Child private IndirectCallNode call;
    @Child private ToArgumentsArrayNode toArgArray;

    public GenericDispatchNode() {
      super(0);
      call = Truffle.getRuntime().createIndirectCallNode();
      toArgArray = ToArgumentsArrayNodeFactory.create(null, null);
    }

    @Override
    public Object executeDispatch(final VirtualFrame frame,
        final Object receiver, final SSymbol selector, final SArray argsArr) {
      SInvokable invokable = Types.getClassOf(receiver).lookupInvokable(selector);

      Object[] arguments = toArgArray.executedEvaluated(argsArr, receiver);

      return call.call(frame, invokable.getCallTarget(), arguments);
    }

    @Override
    public int lengthOfDispatchChain() {
      return 1000;
    }
  }
}
