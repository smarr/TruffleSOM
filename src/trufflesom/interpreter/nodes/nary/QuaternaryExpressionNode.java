package trufflesom.interpreter.nodes.nary;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.vm.NotYetImplementedException;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SSymbol;


@NodeChildren({
    @NodeChild(value = "receiver", type = ExpressionNode.class),
    @NodeChild(value = "firstArg", type = ExpressionNode.class),
    @NodeChild(value = "secondArg", type = ExpressionNode.class),
    @NodeChild(value = "thirdArg", type = ExpressionNode.class)})
public abstract class QuaternaryExpressionNode extends EagerlySpecializableNode {

  public QuaternaryExpressionNode(final SourceSection sourceSection) {
    super(sourceSection);
  }

  public abstract Object executeEvaluated(VirtualFrame frame, Object receiver, Object firstArg,
      Object secondArg, Object thirdArg);

  @Override
  public final Object doPreEvaluated(final VirtualFrame frame,
      final Object[] arguments) {
    return executeEvaluated(frame, arguments[0], arguments[1], arguments[2], arguments[3]);
  }

  @Override
  public EagerPrimitive wrapInEagerWrapper(final SSymbol selector,
      final ExpressionNode[] arguments, final Universe universe) {
    throw new NotYetImplementedException();
  }
}
