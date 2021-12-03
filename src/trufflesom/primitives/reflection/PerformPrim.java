package trufflesom.primitives.reflection;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import bd.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.BinarySystemOperation;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SSymbol;


@Primitive(className = "Object", primitive = "perform:")
public abstract class PerformPrim extends BinarySystemOperation {
  @Child protected AbstractSymbolDispatch dispatch;

  @Override
  public BinarySystemOperation initialize(final Universe universe) {
    super.initialize(universe);
    dispatch = AbstractSymbolDispatchNodeGen.create(sourceSection, universe);
    return this;
  }

  @Specialization
  public final Object doObject(final VirtualFrame frame, final Object receiver,
      final SSymbol selector) {
    return dispatch.executeDispatch(frame, receiver, selector, null);
  }
}
