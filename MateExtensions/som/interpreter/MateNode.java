package som.interpreter;

import com.oracle.truffle.api.nodes.Node;

public interface MateNode {
  /**
   * If necessary, this method wraps the node, and replaces it in the AST with
   * the wrapping node.
   * @return 
   */
  public abstract void wrapIntoMateNode();
  
  default public Node asMateNode() {
    // do nothing!
    // only a small subset of nodes needs to implement this method.
    return null;
  }
}
