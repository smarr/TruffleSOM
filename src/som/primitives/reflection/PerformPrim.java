package som.primitives.reflection;

import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

public abstract class PerformPrim extends BinaryExpressionNode {
  @Child protected SymbolDispatch dispatch;

  public PerformPrim() { super(null); dispatch = SymbolDispatch.create(); }

  @Specialization
  public final Object doObject(final VirtualFrame frame, final Object receiver, final SSymbol selector) {
    return dispatch.executeDispatch(frame, receiver, selector, null);
  }
}
