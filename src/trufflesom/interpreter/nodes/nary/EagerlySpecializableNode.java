package som.interpreter.nodes.nary;

import com.oracle.truffle.api.source.SourceSection;

import bd.primitives.nodes.EagerlySpecializable;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.PreevaluatedExpression;
import som.vm.Universe;
import som.vmobjects.SSymbol;


/**
 * Common root class for node types that are eagerly specializable.
 * The main feature currently provided by this node is the implementation
 * of {@link PreevaluatedExpression}.
 *
 * <p>
 * The main use case at the moment is as common root for primitive nodes, which is used for
 * their specialization.
 */
public abstract class EagerlySpecializableNode extends ExpressionNode
    implements PreevaluatedExpression,
    EagerlySpecializable<ExpressionNode, SSymbol, Universe> {

  @Override
  public ExpressionNode initialize(final SourceSection sourceSection,
      final boolean eagerlyWrapped) {
    initialize(sourceSection);
    return this;
  }
}
