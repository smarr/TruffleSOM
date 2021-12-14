package tools.nodestats;

import com.oracle.truffle.api.instrumentation.Tag;


public final class Tags {
  private Tags() {}

  /** Used to tag every node that can be instrumented. */
  public final class AnyNode extends Tag {
    private AnyNode() {}
  }
}
