package tools.nodestats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.oracle.truffle.api.instrumentation.InstrumentableNode.WrapperNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;


public class NodeStatisticsCollector {

  private final Map<Node, NodeActivation> nodeActivations;

  private final List<AstNode>           fullTrees;
  private final Map<AstNode, Integer>[] partialTrees;

  private final Map<Class<?>, Integer> nodeNumbers;

  private final int maxCandidateTreeHeight;

  private int numberOfNodes;
  private int numberOfNodeClasses;

  @SuppressWarnings({"unchecked", "rawtypes"})
  public NodeStatisticsCollector(final int maxCandidateTreeHeight,
      final Map<Node, NodeActivation> nodeActivations) {
    this.nodeActivations = nodeActivations;
    fullTrees = new ArrayList<>();
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
    AstNode node = collect(root);
    if (node != null) {
      fullTrees.add(node);
    }
  }

  private AstNode collect(final Node node) {
    if (node instanceof WrapperNode) {
      return collect(((WrapperNode) node).getDelegateNode());
    }

    nodeNumbers.merge(node.getClass(), 1, Integer::sum);

    NodeActivation a = nodeActivations.get(node);
    long activations = a != null ? a.getActivations() : 0;
    AstNode ast = new AstNode(node.getClass(), activations);

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

    partialTrees[h - 1].compute(candidate, (existing, i) -> {
      if (existing != candidate) {
        existing.addActivations(candidate);
      }
      if (i == null) {
        return 1;
      }
      return 1 + i;
    });
  }

  public Set<SubTree> getSubTreesWithOccurrenceScore() {
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

  public Set<SubTree> getSubTreesWithActivationScores() {
    Set<SubTree> result = new HashSet<>();

    for (Map<AstNode, Integer> candidatesForHeight : partialTrees) {
      if (candidatesForHeight == null) {
        continue;
      }

      for (Entry<AstNode, Integer> c : candidatesForHeight.entrySet()) {
        AstNode tree = c.getKey();
        assert tree.getHeight() >= 1;
        SubTree candidate = new SubTree(tree, tree.getActivations());
        result.add(candidate);
      }
    }

    return result;
  }
}
