package tools.nodestats;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;


public class NodeActivation extends ExecutionEventNode {
  private long activations;

  public NodeActivation old;

  public long getActivations() {
    long result = activations;
    NodeActivation o = old;
    while (o != null) {
      result += o.activations;
      o = o.old;
    }
    return result;
  }

  @Override
  protected void onEnter(final VirtualFrame frame) {
    activations += 1;
  }
}
