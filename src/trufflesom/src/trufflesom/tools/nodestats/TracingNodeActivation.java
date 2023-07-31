package trufflesom.tools.nodestats;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;


public class TracingNodeActivation extends NodeActivation {
  public static long allActivations;

  private final Node instrumentedNode;

  public TracingNodeActivation(final Node instrumentedNode) {
    this.instrumentedNode = instrumentedNode;
  }

  @Override
  protected void onEnter(final VirtualFrame frame) {
    allActivations += 1;
    super.onEnter(frame);
    CompilerDirectives.transferToInterpreter();
    // Checkstyle: stop
    System.out.println("[NS] " + instrumentedNode.getClass().getName());
    // Checkstyle: resume
  }

  @Override
  public String toString() {
    return "TracingNodeActivation(" + getActivations() + ")";
  }
}
