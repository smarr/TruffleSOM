package som.interpreter;


public interface MateNode {
  /**
   * If necessary, this method wraps the node, and replaces it in the AST with
   * the wrapping node.
   * @return 
   */
  default void wrapIntoMateNode(){};
}
