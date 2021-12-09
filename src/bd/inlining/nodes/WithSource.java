package bd.inlining.nodes;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInterface;


/**
 * All nodes that are handled by inlining are expected to implement {@link WithSource}, which
 * is used to make sure they have the source section attribution after inlining.
 */
public interface WithSource extends NodeInterface {

  /**
   * Initialize the node with the source section.
   *
   * @param <T> the type of node
   *
   * @param sourceCoord the SourceCoordinate encoded char index and length
   * @return the node itself
   */
  <T extends Node> T initialize(long sourceCoord);

  long getSourceCoordinate();
}
