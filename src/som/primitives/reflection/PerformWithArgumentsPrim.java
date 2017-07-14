package som.primitives.reflection;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.source.SourceSection;

import bd.primitives.Primitive;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.vm.Universe;
import som.vmobjects.SArray;
import som.vmobjects.SSymbol;


@GenerateNodeFactory
@Primitive(className = "Object", primitive = "perform:withArguments:", requiresContext = true)
public abstract class PerformWithArgumentsPrim extends TernaryExpressionNode {

  @Child protected AbstractSymbolDispatch dispatch;

  public PerformWithArgumentsPrim(final SourceSection source, final Universe universe) {
    super(source);
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
