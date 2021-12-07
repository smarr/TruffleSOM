package trufflesom.primitives.reflection;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.source.SourceSection;

import bd.primitives.Primitive;
import trufflesom.interpreter.nodes.nary.TernaryExpressionNode;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SSymbol;


@GenerateNodeFactory
@Primitive(className = "Object", primitive = "perform:withArguments:")
public abstract class PerformWithArgumentsPrim extends TernaryExpressionNode {

  @Child protected AbstractSymbolDispatch dispatch;

  @Override
  public PerformWithArgumentsPrim initialize(final SourceSection sourceSection) {
    super.initialize(sourceSection);
    dispatch = AbstractSymbolDispatchNodeGen.create(sourceSection);
    return this;
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
