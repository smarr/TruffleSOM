package som.primitives.reflection;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.vm.Universe;
import som.vmobjects.SSymbol;


@GenerateNodeFactory
public abstract class PerformPrim extends BinaryExpressionNode {
  @Child protected AbstractSymbolDispatch dispatch;

  public PerformPrim(final Universe universe) {
    super(null);
    dispatch = AbstractSymbolDispatchNodeGen.create(universe);
  }

  @Specialization
  public final Object doObject(final VirtualFrame frame, final Object receiver,
      final SSymbol selector) {
    return dispatch.executeDispatch(frame, receiver, selector, null);
  }
}
