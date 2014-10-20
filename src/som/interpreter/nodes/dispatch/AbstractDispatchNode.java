package som.interpreter.nodes.dispatch;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;


public abstract class AbstractDispatchNode extends Node {
  public abstract Object executeDispatch(
      final VirtualFrame frame, final Object[] arguments);
}
