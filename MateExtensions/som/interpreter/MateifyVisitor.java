package som.interpreter;

import java.util.List;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.NodeVisitor;

import som.interpreter.nodes.MateNode;
import som.interpreter.nodes.SOMNode;


public class MateifyVisitor implements NodeVisitor {

  public boolean visit(Node node) {
    if (node instanceof SOMNode) {
      Node matenode = ((SOMNode) node).wrapIntoMateNode();
      node.replace(matenode);
    }
    return true;
  }

}