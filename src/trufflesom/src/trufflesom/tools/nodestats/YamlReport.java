package trufflesom.tools.nodestats;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;


public class YamlReport {
  private static final class ScoredAlphaOrder implements Comparator<Entry<Class<?>, Integer>> {
    @Override
    public int compare(final Entry<Class<?>, Integer> e1, final Entry<Class<?>, Integer> e2) {
      int score = e2.getValue() - e1.getValue();
      if (score != 0) {
        return score;
      }

      return e1.getKey().getName().compareTo(e2.getKey().getName());
    }
  }

  public static final class ScoredAlphabeticRootOrder implements Comparator<SubTree> {
    @Override
    public int compare(final SubTree o1, final SubTree o2) {
      long score = o2.score - o1.score;
      if (score != 0) {
        return (int) score;
      }

      return o1.compareTo(o2);
    }
  }

  private static void reportNodeNumbers(final NodeStatisticsCollector collector,
      final StringBuilder builder, final String indent) {
    Map<Class<?>, Integer> allNodes = collector.getNodeNumbers();

    int maxNameLength = allNodes.keySet().stream()
                                .map(c -> c.getName())
                                .mapToInt(String::length)
                                .max()
                                .orElse(0);

    List<Entry<Class<?>, Integer>> nodes = allNodes.entrySet().stream()
                                                   .sorted(new ScoredAlphaOrder())
                                                   .collect(Collectors.toList());

    builder.append(indent);
    builder.append("node-numbers:\n");

    for (Entry<Class<?>, Integer> e : nodes) {
      builder.append(indent);
      builder.append(indent);
      builder.append('-');
      builder.append(' ');

      String name = e.getKey().getName();
      int length = name.length();

      builder.append(name);
      builder.append(':');

      for (int i = 0; i < maxNameLength - length; i += 1) {
        builder.append(' ');
      }

      builder.append("  { score: ");
      builder.append(e.getValue());
      builder.append(" }\n");
    }
  }

  private static void reportStaticSubTrees(final NodeStatisticsCollector collector,
      final StringBuilder builder, final String indent) {
    builder.append(indent);
    builder.append("subtree-occurrences:\n");

    Set<SubTree> cs = collector.getSubTreesWithOccurrenceScore();
    final List<SubTree> sorted = cs.stream()
                                   .sorted(new ScoredAlphabeticRootOrder())
                                   .collect(Collectors.toList());

    for (SubTree c : sorted) {
      c.yamlPrint(builder, indent, 2);
      builder.append('\n');
    }
  }

  private static void reportDynamicSubTrees(final NodeStatisticsCollector collector,
      final StringBuilder builder, final String indent) {
    builder.append(indent);
    builder.append("subtree-activations:\n");

    Set<SubTree> cs = collector.getSubTreesWithActivationScores();
    final List<SubTree> sorted = cs.stream()
                                   .sorted(new ScoredAlphabeticRootOrder())
                                   .collect(Collectors.toList());

    for (SubTree c : sorted) {
      c.yamlPrint(builder, indent, 2);
      builder.append('\n');
    }
  }

  public static String createReport(final NodeStatisticsCollector collector) {
    StringBuilder builder = new StringBuilder();

    builder.append("# Node Statistics Report\n");
    builder.append("report:\n");

    reportNodeNumbers(collector, builder, "  ");

    builder.append('\n');
    builder.append('\n');

    reportStaticSubTrees(collector, builder, "  ");

    builder.append('\n');
    builder.append('\n');

    reportDynamicSubTrees(collector, builder, "  ");

    return builder.toString();
  }
}
