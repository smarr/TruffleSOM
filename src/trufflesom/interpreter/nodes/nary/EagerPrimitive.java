package trufflesom.interpreter.nodes.nary;

import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.PreevaluatedExpression;
import trufflesom.tools.Send;


public abstract class EagerPrimitive extends ExpressionNode
    implements PreevaluatedExpression, Send {}
