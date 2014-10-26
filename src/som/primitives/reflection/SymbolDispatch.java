package som.primitives.reflection;

import som.interpreter.SArguments;
import som.interpreter.Types;
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

  private SymbolDispatch() {
    call = Truffle.getRuntime().createIndirectCallNode();
  }

  public Object executeDispatch(final VirtualFrame frame,
      final Object receiver, final SSymbol selector, final Object[] argsArr) {
    SInvokable invokable = Types.getClassOf(receiver).lookupInvokable(selector);

    Object[] args = SArguments.createSArgumentsArrayFrom(receiver, argsArr);

    return call.call(frame, invokable.getCallTarget(), args);
  }
}
