package tools.nodestats;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;


public class NodeActivation extends ExecutionEventNode {
  private final Class<?> nodeTargetClass;

  private long activations;

  public NodeActivation(final Class<?> nodeTargetClass) {
    this.nodeTargetClass = nodeTargetClass;
  }

  @Override
  protected void onEnter(final VirtualFrame frame) {
    activations += 1;
  }
}
