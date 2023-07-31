package trufflesom.interpreter.nodes.nary;

import trufflesom.bdt.primitives.nodes.PreevaluatedExpression;
import trufflesom.interpreter.nodes.ExpressionNode;


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
    implements PreevaluatedExpression {}
