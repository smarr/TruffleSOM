package som.primitives.reflection;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;

import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.vm.Universe;
import som.vmobjects.SArray;
import som.vmobjects.SSymbol;


@GenerateNodeFactory
public abstract class PerformWithArgumentsPrim extends TernaryExpressionNode {

  @Child protected AbstractSymbolDispatch dispatch;

  public PerformWithArgumentsPrim(final Universe universe) {
    dispatch = AbstractSymbolDispatchNodeGen.create(universe);
  }

  @Specialization
  public final Object doObject(final VirtualFrame frame,
      final Object receiver, final SSymbol selector, final SArray argsArr) {
    return dispatch.executeDispatch(frame, receiver, selector, argsArr);
  }

  @Override
  public NodeCost getCost() {
    return dispatch.getCost();
  }
}
