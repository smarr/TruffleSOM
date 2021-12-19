package tools.nodestats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class AstNode implements Comparable<AstNode> {
  private final Class<?> nodeClass;
  private List<AstNode>  children;

  private long activations;

  private int height;

  private int hashcode;

  public AstNode(final Class<?> nodeClass, final long activations) {
    this.nodeClass = nodeClass;
    height = -1;
    hashcode = -1;
    this.activations = activations;
  }

  private AstNode(final Class<?> nodeClass, final long activations,
      final List<AstNode> children, final int height) {
    this.nodeClass = nodeClass;
    this.children = children;
    this.height = height;
    hashcode = -1;
    this.activations = activations;
  }

  public long getActivations() {
    return activations;
  }

  public void addChild(final AstNode child) {
    hashcode = -1;
    if (children == null) {
      children = new ArrayList<>(3);
    }

    children.add(child);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AstNode a = (AstNode) o;
    if (nodeClass != a.nodeClass) {
      return false;
    }

    if (children == a.children) {
      return true;
    }

    if (children == null || a.children == null) {
      return false;
    }

    if (children.size() != a.children.size()) {
      return false;
    }

    for (int i = 0; i < children.size(); i += 1) {
      if (!children.get(i).equals(a.children.get(i))) {
        return false;
      }
    }

    return true;
  }

  @Override
  public int hashCode() {
    if (hashcode != -1) {
      return hashcode;
    }

    if (children == null) {
      hashcode = nodeClass.hashCode();
      return hashcode;
    }

    Object[] hashingObjects = new Object[1 + children.size()];
    hashingObjects[0] = nodeClass;

    for (int i = 0; i < children.size(); i += 1) {
      hashingObjects[i + 1] = children.get(i);
    }

    hashcode = Arrays.hashCode(hashingObjects);
    return hashcode;
  }

  public int collectTreesAndDetermineHeight(final int maxCandidateTreeHeight,
      final NodeStatisticsCollector collector) {
    if (children == null) {
      height = 0;
      return 0;
    }

    int maxChildDepth = 0;
    for (AstNode c : children) {
      maxChildDepth = Math.max(maxChildDepth,
          c.collectTreesAndDetermineHeight(maxCandidateTreeHeight, collector));
    }

    height = maxChildDepth + 1;

    if (height >= 1 && collector != null) {
      for (int h = 1; h <= maxCandidateTreeHeight && h <= height; h += 1) {
        collector.addCandidate(cloneWithMaxHeight(h));
      }
    }

    return height;
  }

  public AstNode cloneWithMaxHeight(final int maxTreeHeight) {
    assert height >= 0;
    if (maxTreeHeight >= height || height == 0) {
      return this;
    }

    assert height > 0 && children != null && children.size() > 0;

    ArrayList<AstNode> clonedChildren;
    if (maxTreeHeight == 0) {
      clonedChildren = null;
    } else {
      clonedChildren = new ArrayList<>(children.size());
      for (AstNode c : children) {
        clonedChildren.add(c.cloneWithMaxHeight(maxTreeHeight - 1));
      }
    }

    AstNode clone = new AstNode(nodeClass, activations, clonedChildren, maxTreeHeight);
    return clone;
  }

  public List<AstNode> getChildren() {
    return children;
  }

  public Class<?> getNodeClass() {
    return nodeClass;
  }

  public int getHeight() {
    return height;
  }

  public void yamlPrint(final StringBuilder builder, final String indent, final int level) {
    builder.append(nodeClass.getSimpleName());

    builder.append(":\n");

    for (int i = 0; i < level; i += 1) {
      builder.append(indent);
    }

    builder.append("- activations: ");
    builder.append(activations);
    builder.append('\n');

    if (children == null) {
      return;
    }

    for (AstNode c : children) {
      for (int i = 0; i < level; i += 1) {
        builder.append(indent);
      }

      builder.append("- ");

      c.yamlPrint(builder, indent, level + 1);
    }
  }

  @Override
  public String toString() {
    return "Node(" + nodeClass.getSimpleName() + ", " + height + ")";
  }

  public void addActivations(final AstNode candidate) {
    activations += candidate.activations;

    assert children.size() == candidate.children.size();
    for (int i = 0; i < children.size(); i += 1) {
      children.get(i).addActivations(candidate.children.get(i));
    }
  }

  @Override
  public int compareTo(final AstNode o) {
    int diff = o.height - height;
    if (diff != 0) {
      return diff;
    }

    diff = (int) (o.activations - activations);
    if (diff != 0) {
      return diff;
    }

    diff = nodeClass.getName().compareTo(o.nodeClass.getName());
    if (diff != 0) {
      return diff;
    }

    if (children == null && o.children != null) {
      return -1;
    } else if (o.children == null && children != null) {
      return 1;
    } else if (children == null && o.children == null) {
      return 0;
    }

    diff = children.size() - o.children.size();
    if (diff != 0) {
      return diff;
    }

    for (int i = 0; i < children.size(); i += 1) {
      diff = children.get(i).compareTo(o.children.get(i));
      if (diff != 0) {
        return diff;
      }
    }

    return 0;
  }
}
