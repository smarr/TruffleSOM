package som.primitives.reflection;

import java.util.Iterator;

import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;


@GenerateNodeFactory
public abstract class PerformPrim extends BinaryExpressionNode {
  @Child protected AbstractSymbolDispatch dispatch;

  public PerformPrim() { super(null); dispatch = AbstractSymbolDispatchNodeGen.create(); }

  @Specialization
  public final Object doObject(final VirtualFrame frame, final Object receiver, final SSymbol selector) {
    return dispatch.
        executeDispatch(frame, receiver, selector, null);
  }
  
  @Override
  public Object[] evaluateArguments(VirtualFrame frame) {
    Object[] arguments = new Object[2];
    Iterator<Node> it = this.getChildren().iterator();
    it.next();
    arguments[0] = ((ExpressionNode)it.next()).executeGeneric(frame);
    arguments[1] = ((ExpressionNode)it.next()).executeGeneric(frame);
    return arguments;
  }
}
