package som.interpreter.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;


public abstract class ExpressionWithReceiverNode extends ExpressionNode {

  public ExpressionWithReceiverNode(final SourceSection sourceSection) {
    super(sourceSection);
  }

  public abstract ExpressionNode getReceiver();

  public Object evaluateReceiver(final VirtualFrame frame){
    return this.getReceiver().executeGeneric(frame);
  }
}


