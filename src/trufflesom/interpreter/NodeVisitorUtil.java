package trufflesom.interpreter;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeVisitor;

import trufflesom.interpreter.nodes.ExpressionNode;


public final class NodeVisitorUtil {

  private static final class DummyParent extends Node {
    private DummyParent() {
      super();
    }

    @Child private ExpressionNode child;

    private void adopt(final ExpressionNode child) {
      this.child = insert(child);
    }
  }

  public static ExpressionNode applyVisitor(final ExpressionNode body,
      final NodeVisitor visitor) {
    DummyParent dummyParent = new DummyParent();
    dummyParent.adopt(body);
    body.accept(visitor);

    // need to return the child of the dummy parent,
    // since it could have been replaced
    return dummyParent.child;
  }
}
