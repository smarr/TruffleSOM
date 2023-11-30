package trufflesom.primitives.reflection;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import trufflesom.bdt.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.BinaryExpressionNode;
import trufflesom.vmobjects.SSymbol;


@Primitive(className = "Object", primitive = "perform:")
@GenerateNodeFactory
public abstract class PerformPrim extends BinaryExpressionNode {
  @Child protected AbstractSymbolDispatch dispatch;

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Node> T initialize(final long coord) {
    super.initialize(coord);
    dispatch = AbstractSymbolDispatchNodeGen.create(coord);
    return (T) this;
  }

  @Specialization
  public final Object doObject(final VirtualFrame frame, final Object receiver,
      final SSymbol selector) {
    return dispatch.executeDispatch(frame, receiver, selector, null);
  }
}
