package trufflesom.interpreter.nodes.nary;

import bd.tools.nodes.Invocation;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.PreevaluatedExpression;
import trufflesom.vmobjects.SSymbol;


public abstract class EagerPrimitive extends ExpressionNode
    implements PreevaluatedExpression, Invocation<SSymbol> {}
