package som.interpreter;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeVisitor;


public class MateifyVisitor implements NodeVisitor {

  @Override
  public boolean visit(final Node node) {
    if (node instanceof MateNode) {
      ((MateNode) node).wrapIntoMateNode();
    }
    return true;
  }

}