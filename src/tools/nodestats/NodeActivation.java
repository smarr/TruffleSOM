package tools.nodestats;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;


public class NodeActivation extends ExecutionEventNode {
  private long activations;

  public long getActivations() {
    return activations;
  }

  @Override
  protected void onEnter(final VirtualFrame frame) {
    activations += 1;
  }
}
