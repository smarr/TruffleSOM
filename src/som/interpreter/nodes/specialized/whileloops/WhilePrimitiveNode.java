package som.interpreter.nodes.specialized.whileloops;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import bd.primitives.Primitive;
import som.interpreter.nodes.nary.BinaryExpressionNode.BinarySystemOperation;
import som.vm.Universe;
import som.vmobjects.SBlock;
import som.vmobjects.SObject;


@GenerateNodeFactory
public abstract class WhilePrimitiveNode extends BinarySystemOperation {
  final boolean predicateBool;

  @Child protected WhileCache whileNode;

  protected WhilePrimitiveNode(final boolean predicateBool) {
    this.predicateBool = predicateBool;

  }

  @Override
  public WhilePrimitiveNode initialize(final Universe universe) {
    super.initialize(universe);
    this.whileNode = WhileCacheNodeGen.create(predicateBool, universe, null, null)
                                      .initialize(sourceSection);
    return this;
  }

  @Specialization
  protected SObject doWhileConditionally(final VirtualFrame frame,
      final SBlock loopCondition, final SBlock loopBody) {
    return (SObject) whileNode.executeEvaluated(frame, loopCondition, loopBody);
  }

  @Primitive(className = "Block", primitive = "whileTrue:", selector = "whileTrue:",
      receiverType = SBlock.class, noWrapper = true)
  // TODO: need to check for the second argument, check WhileSplzr
  public abstract static class WhileTruePrimitiveNode extends WhilePrimitiveNode {
    public WhileTruePrimitiveNode() {
      super(true);
    }
  }

  @Primitive(className = "Block", primitive = "whileFalse:", selector = "whileFalse:",
      receiverType = SBlock.class, noWrapper = true)
  // TODO: need to check for the second argument, check WhileSplzr
  public abstract static class WhileFalsePrimitiveNode extends WhilePrimitiveNode {
    public WhileFalsePrimitiveNode() {
      super(false);
    }
  }
}
