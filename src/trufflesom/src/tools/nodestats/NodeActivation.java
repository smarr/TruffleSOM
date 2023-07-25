package tools.nodestats;

import java.util.Set;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;


public class NodeActivation extends ExecutionEventNode {
  private long activations;

  public NodeActivation old;

  public void addOld(final Set<NodeActivation> activations) {
    NodeActivation o = old;
    while (o != null) {
      activations.add(o);
      o = o.old;
    }
  }

  public long getActivations() {
    return activations;
  }

  @Override
  protected void onEnter(final VirtualFrame frame) {
    activations += 1;
  }
}
