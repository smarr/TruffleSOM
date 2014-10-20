package som.primitives.reflection;

import som.interpreter.SArguments;
import som.interpreter.Types;
import som.primitives.arrays.ToArgumentsArrayNode;
import som.primitives.arrays.ToArgumentsArrayNodeFactory;
import som.vmobjects.SArray;
import som.vmobjects.SInvokable;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;


public final class SymbolDispatch extends Node {

  public static SymbolDispatch create() {
    return new SymbolDispatch();
  }

  @Child private IndirectCallNode call;
    @Child private ToArgumentsArrayNode toArgArray;

  private SymbolDispatch() {
    call = Truffle.getRuntime().createIndirectCallNode();
      toArgArray = ToArgumentsArrayNodeFactory.create(null, null);
  }

  public Object executeDispatch(final VirtualFrame frame,
        final Object receiver, final SSymbol selector, final SArray argsArr) {
    SInvokable invokable = Types.getClassOf(receiver).lookupInvokable(selector);

      Object[] arguments = toArgArray.executedEvaluated(argsArr, receiver);

      return call.call(frame, invokable.getCallTarget(), arguments);
  }
}
