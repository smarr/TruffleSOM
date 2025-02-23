package trufflesom.interpreter.nodes.nary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.frame.VirtualFrame;

import com.oracle.truffle.api.nodes.Node;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.vmobjects.SSymbol;


public abstract class QuaternaryMsgExprNode extends QuaternaryExpressionNode {
  public abstract SSymbol getSelector();

  public static AbstractDispatchNode createDispatch(Node node) {
    QuaternaryMsgExprNode n = (QuaternaryMsgExprNode) node;
    return AbstractDispatchNode.create(n.getSelector());
  }

  @Fallback
  public static final Object makeGenericSend(final VirtualFrame frame,
      final Object receiver, final Object arg1, final Object arg2, final Object arg3,
      @Bind Node self, @Cached("createDispatch(self)") final AbstractDispatchNode dispatch) {
    return dispatch.executeDispatch(frame, new Object[] {receiver, arg1, arg2, arg3});
  }
}
