package som.interpreter.nodes.nary;

import som.interpreter.nodes.MateAbstractReflectiveDispatch;
import som.interpreter.nodes.MateAbstractSemanticNodes.MateSemanticCheckNode;
import com.oracle.truffle.api.frame.VirtualFrame;


public interface MateMessage {
  
  public abstract MateSemanticCheckNode getMateNode();
  public abstract MateAbstractReflectiveDispatch getMateDispatch();
  //public abstract Object doPreEvaluated(final VirtualFrame frame, final Object[] arguments);
  
  public default Object doMateSemantics(final VirtualFrame frame,
      final Object[] arguments) {
      return this.getMateDispatch().executeDispatch(frame, this.getMateNode().execute(frame, arguments), arguments);
  }
}
