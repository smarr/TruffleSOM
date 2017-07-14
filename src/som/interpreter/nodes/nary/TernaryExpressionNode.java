package som.interpreter.nodes.nary;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import som.interpreter.nodes.ExpressionNode;
import som.vm.Universe;
import som.vmobjects.SSymbol;


@NodeChildren({
    @NodeChild(value = "receiver", type = ExpressionNode.class),
    @NodeChild(value = "firstArg", type = ExpressionNode.class),
    @NodeChild(value = "secondArg", type = ExpressionNode.class)})
public abstract class TernaryExpressionNode extends EagerlySpecializableNode {

  public TernaryExpressionNode(final SourceSection sourceSection) {
    super(sourceSection);
  }

  public abstract Object executeEvaluated(VirtualFrame frame, Object receiver, Object firstArg,
      Object secondArg);

  @Override
  public final Object doPreEvaluated(final VirtualFrame frame,
      final Object[] arguments) {
    return executeEvaluated(frame, arguments[0], arguments[1], arguments[2]);
  }

  @Override
  public EagerPrimitive wrapInEagerWrapper(final SSymbol selector,
      final ExpressionNode[] arguments, final Universe universe) {
    return new EagerTernaryPrimitiveNode(sourceSection, selector,
        arguments[0], arguments[1], arguments[2], this, universe);
  }
}
