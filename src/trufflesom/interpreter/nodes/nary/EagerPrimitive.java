package trufflesom.interpreter.nodes.nary;

import bd.primitives.nodes.PreevaluatedExpression;
import bd.tools.nodes.Invocation;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.vmobjects.SSymbol;


public abstract class EagerPrimitive extends ExpressionNode
    implements PreevaluatedExpression, Invocation<SSymbol> {
}
