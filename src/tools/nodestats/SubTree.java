package tools.nodestats;

import java.util.Comparator;


final class SubTree {
  private final AstNode rootNode;
  private final int     score;

  SubTree(final AstNode rootNode, final int score) {
    this.rootNode = rootNode;
    this.score = score;
  }

  public AstNode getRoot() {
    return rootNode;
  }

  public int getScore() {
    return score;
  }

  public String prettyPrint() {
    StringBuilder builder = new StringBuilder();
    rootNode.prettyPrint(builder, 0);
    return builder.toString();
  }

  public void prettyPrint(final StringBuilder builder) {
    builder.append("score: ");
    builder.append(score + " ");
    rootNode.prettyPrint(builder, 0);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SubTree candidate = (SubTree) o;
    return rootNode.equals(candidate.rootNode);
  }

  @Override
  public int hashCode() {
    return rootNode.hashCode();
  }

  @Override
  public String toString() {
    return "Candidate(" + score + ": " + rootNode.getNodeClass().getSimpleName() + ", "
        + rootNode.getHeight() + ")";
  }

  public static final class ScoredAlphabeticRootOrder implements Comparator<SubTree> {
    @Override
    public int compare(final SubTree o1, final SubTree o2) {
      int score = o2.score - o1.score;
      if (score != 0) {
        return score;
      }

      return o1.getClass().getSimpleName().compareTo(o2.getClass().getSimpleName());
    }
  }
}
