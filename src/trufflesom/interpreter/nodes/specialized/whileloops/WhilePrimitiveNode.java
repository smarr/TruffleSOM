package som.interpreter.nodes.specialized.whileloops;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.primitives.Primitive;
import som.vm.Universe;
import som.vmobjects.SBlock;
import som.vmobjects.SObject;


@GenerateNodeFactory
public abstract class WhilePrimitiveNode extends BinaryExpressionNode {
  final boolean               predicateBool;
  @Child protected WhileCache whileNode;

  protected WhilePrimitiveNode(final SourceSection source, final boolean predicateBool,
      final Universe universe) {
    super(source);
    this.predicateBool = predicateBool;
    this.whileNode = WhileCacheNodeGen.create(source, predicateBool, universe, null, null);
  }

  protected WhilePrimitiveNode(final WhilePrimitiveNode node, final Universe universe) {
    this(node.sourceSection, node.predicateBool, universe);
  }

  @Specialization
  protected SObject doWhileConditionally(final VirtualFrame frame,
      final SBlock loopCondition, final SBlock loopBody) {
    return (SObject) whileNode.executeEvaluated(frame, loopCondition, loopBody);
  }

  @Primitive(className = "Block", primitive = "whileTrue:", selector = "whileTrue:",
      receiverType = SBlock.class, noWrapper = true, requiresContext = true)
  // TODO: need to check for the second argument, check WhileSplzr
  public abstract static class WhileTruePrimitiveNode extends WhilePrimitiveNode {
    public WhileTruePrimitiveNode(final SourceSection source, final Universe universe) {
      super(source, true, universe);
    }
  }

  @Primitive(className = "Block", primitive = "whileFalse:", selector = "whileFalse:",
      receiverType = SBlock.class, noWrapper = true, requiresContext = true)
  // TODO: need to check for the second argument, check WhileSplzr
  public abstract static class WhileFalsePrimitiveNode extends WhilePrimitiveNode {
    public WhileFalsePrimitiveNode(final SourceSection source, final Universe universe) {
      super(source, false, universe);
    }
  }
}
