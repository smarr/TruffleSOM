package trufflesom.primitives.reflection;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import bdt.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.BinaryExpressionNode;
import trufflesom.vmobjects.SSymbol;


@Primitive(className = "Object", primitive = "perform:")
@GenerateNodeFactory
public abstract class PerformPrim extends BinaryExpressionNode {
  @Child protected AbstractSymbolDispatch dispatch;

  @SuppressWarnings("unchecked")
  @Override
  public PerformPrim initialize(final long coord) {
    super.initialize(coord);
    dispatch = AbstractSymbolDispatchNodeGen.create(coord);
    return this;
  }

  @Specialization
  public final Object doObject(final VirtualFrame frame, final Object receiver,
      final SSymbol selector) {
    return dispatch.executeDispatch(frame, receiver, selector, null);
  }
}
