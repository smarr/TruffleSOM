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
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;


public class NodeStatisticsCollector {

  private static final class PartialPair {
    final AstNode tree;

    int occurrences;

    PartialPair(final AstNode tree) {
      this.tree = tree;
      this.occurrences = 1;
    }
  }

  private final Map<WrapperNode, NodeActivation> nodeActivations;

  private final List<AstNode>               fullTrees;
  private final Map<AstNode, PartialPair>[] partialTrees;

  private final Map<Class<?>, Integer> nodeNumbers;

  private final int maxCandidateTreeHeight;

  private int numberOfNodes;
  private int numberOfNodeClasses;

  @SuppressWarnings({"unchecked", "rawtypes"})
  public NodeStatisticsCollector(final int maxCandidateTreeHeight,
      final Map<Node, NodeActivation> nodeActivations) {
    this.nodeActivations = toWrapperMap(nodeActivations);
    fullTrees = new ArrayList<>();
    nodeNumbers = new HashMap<>();

    partialTrees = new Map[maxCandidateTreeHeight];
    this.maxCandidateTreeHeight = maxCandidateTreeHeight;
  }

  @SuppressWarnings("unlikely-arg-type")
  private Map<WrapperNode, NodeActivation> toWrapperMap(
      final Map<Node, NodeActivation> nodeActivations) {
    Map<WrapperNode, NodeActivation> result = new HashMap<>();

    for (Entry<Node, NodeActivation> e : nodeActivations.entrySet()) {
      Node wrapper = e.getKey().getParent();
      assert wrapper instanceof WrapperNode;

      NodeActivation old = result.get(wrapper);
      if (old != null) {
        if (e.getValue().old == null) {
          e.getValue().old = old;
        } else {
          assert old.old == null; // TODO: just need to append it in either case to the last
                                  // node...
          old.old = e.getValue();
          continue;
        }
      }
      result.put((WrapperNode) wrapper, e.getValue());
    }

    return result;
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

  @SuppressWarnings("unlikely-arg-type")
  private AstNode collect(final Node node) {
    if (node instanceof WrapperNode) {
      return collect(((WrapperNode) node).getDelegateNode());
    }

    nodeNumbers.merge(node.getClass(), 1, Integer::sum);
    assert node.getParent() instanceof WrapperNode || node instanceof RootNode
        || node instanceof DirectCallNode;

    Node lookupNode = node.getParent();
    if (!(lookupNode instanceof WrapperNode)) {
      lookupNode = node;
    }

    NodeActivation a = nodeActivations.get(lookupNode);

    // assert a != null || node instanceof RootNode || node instanceof DirectCallNode;
    AstNode ast = new AstNode(node.getClass(), a);

    for (Node c : node.getChildren()) {
      AstNode child = collect(c);
      ast.addChild(child);
    }

    ast.sortChildren();

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

    partialTrees[h - 1].compute(candidate, (key, pair) -> {
      if (pair == null) {
        assert candidate == key;
        return new PartialPair(key);
      }

      pair.tree.addActivations(candidate);
      pair.occurrences += 1;

      return pair;
    });
  }

  public Set<SubTree> getSubTreesWithOccurrenceScore() {
    Set<SubTree> result = new HashSet<>();

    for (Map<AstNode, PartialPair> candidatesForHeight : partialTrees) {
      if (candidatesForHeight == null) {
        continue;
      }

      for (PartialPair c : candidatesForHeight.values()) {
        assert c.tree.getHeight() >= 1;
        SubTree candidate = new SubTree(c.tree, c.occurrences);
        result.add(candidate);
      }
    }

    return result;
  }

  public Set<SubTree> getSubTreesWithActivationScores() {
    Set<SubTree> result = new HashSet<>();

    for (Map<AstNode, PartialPair> candidatesForHeight : partialTrees) {
      if (candidatesForHeight == null) {
        continue;
      }

      for (PartialPair c : candidatesForHeight.values()) {
        AstNode tree = c.tree;
        assert tree.getHeight() >= 1;
        SubTree candidate = new SubTree(tree, tree.getNumActivations());
        result.add(candidate);
      }
    }

    return result;
  }
}
