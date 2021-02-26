package trufflesom.interpreter.nodes.specialized.whileloops;

import trufflesom.interpreter.nodes.nary.BinaryExpressionNode.BinarySystemOperation;
import trufflesom.vm.Universe;


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
}
