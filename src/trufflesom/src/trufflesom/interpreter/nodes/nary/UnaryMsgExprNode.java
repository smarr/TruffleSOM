package trufflesom.interpreter.nodes.nary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.frame.VirtualFrame;

import com.oracle.truffle.api.nodes.Node;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.vmobjects.SSymbol;


public abstract class UnaryMsgExprNode extends UnaryExpressionNode {
  public abstract SSymbol getSelector();

  public static AbstractDispatchNode createDispatch(Node node) {
    UnaryMsgExprNode n = (UnaryMsgExprNode) node;
    return AbstractDispatchNode.create(n.getSelector());
  }

  @Fallback
  public static final Object genericSend(final VirtualFrame frame, final Object receiver,
      @Bind Node self,
      @Cached("createDispatch(self)") final AbstractDispatchNode dispatch) {
    return dispatch.executeDispatch(frame, new Object[] {receiver});
  }
}
