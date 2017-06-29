package som.interpreter.nodes.specialized.whileloops;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.vm.Universe;
import som.vmobjects.SBlock;
import som.vmobjects.SObject;


@GenerateNodeFactory
public abstract class WhilePrimitiveNode extends BinaryExpressionNode {
  final boolean               predicateBool;
  @Child protected WhileCache whileNode;

  protected WhilePrimitiveNode(final boolean predicateBool, final Universe universe) {
    super(null);
    this.predicateBool = predicateBool;
    this.whileNode = WhileCacheNodeGen.create(predicateBool, universe, null, null);
  }

  protected WhilePrimitiveNode(final WhilePrimitiveNode node, final Universe universe) {
    this(node.predicateBool, universe);
  }

  @Specialization
  protected SObject doWhileConditionally(final VirtualFrame frame,
      final SBlock loopCondition, final SBlock loopBody) {
    return (SObject) whileNode.executeEvaluated(frame, loopCondition, loopBody);
  }

  public abstract static class WhileTruePrimitiveNode extends WhilePrimitiveNode {
    public WhileTruePrimitiveNode(final Universe universe) {
      super(true, universe);
    }
  }

  public abstract static class WhileFalsePrimitiveNode extends WhilePrimitiveNode {
    public WhileFalsePrimitiveNode(final Universe universe) {
      super(false, universe);
    }
  }
}
