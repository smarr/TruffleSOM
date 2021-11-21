package tools.nodestats;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;


public class NodeStatisticsCollector {

  private final Set<AstNode>            fullTrees;
  private final Map<AstNode, Integer>[] partialTrees;

  private final Map<Class<?>, Integer> nodeNumbers;

  private final int maxCandidateTreeHeight;

  private int numberOfNodes;
  private int numberOfNodeClasses;

  @SuppressWarnings({"unchecked", "rawtypes"})
  public NodeStatisticsCollector(final int maxCandidateTreeHeight) {
    fullTrees = new HashSet<>();
    nodeNumbers = new HashMap<>();

    partialTrees = new Map[maxCandidateTreeHeight];
    this.maxCandidateTreeHeight = maxCandidateTreeHeight;
  }

  public void addAll(final Collection<RootNode> roots) {
    for (RootNode root : roots) {
      add(root);
    }
  }

  public Map<Class<?>, Integer> getNodeNumbers() {
    return nodeNumbers;
  }

  public int getNumberOfNodes() {
    if (numberOfNodes == 0) {
      numberOfNodes = nodeNumbers.values().stream().reduce(0, Integer::sum);
    }
    return numberOfNodes;
  }

  public int getNumberOfNodeTypes() {
    if (numberOfNodeClasses == 0) {
      numberOfNodeClasses = nodeNumbers.size();
    }
    return numberOfNodeClasses;
  }

  public void add(final RootNode root) {
    fullTrees.add(collect(root));
  }

  private AstNode collect(final Node node) {
    nodeNumbers.merge(node.getClass(), 1, Integer::sum);

    AstNode ast = new AstNode(node.getClass());

    for (Node c : node.getChildren()) {
      AstNode child = collect(c);
      ast.addChild(child);
    }

    return ast;
  }

  public void collectStats() {
    for (AstNode tree : fullTrees) {
      tree.collectTreesAndDetermineHeight(maxCandidateTreeHeight, this);
    }
  }

  public void addCandidate(final AstNode candidate) {
    int h = candidate.getHeight();
    assert h >= 1;
    if (partialTrees[h - 1] == null) {
      partialTrees[h - 1] = new HashMap<>();
    }

    partialTrees[h - 1].merge(candidate, 1, Integer::sum);
  }

  public Set<SubTree> getSubTrees() {
    Set<SubTree> result = new HashSet<>();

    for (Map<AstNode, Integer> candidatesForHeight : partialTrees) {
      if (candidatesForHeight == null) {
        continue;
      }

      for (Entry<AstNode, Integer> c : candidatesForHeight.entrySet()) {
        assert c.getKey().getHeight() >= 1;
        SubTree candidate = new SubTree(c.getKey(), c.getValue());
        result.add(candidate);
      }
    }

    return result;
  }
}
