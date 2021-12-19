package tools.nodestats;

import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionKey;
import org.graalvm.options.OptionStability;

import com.oracle.truffle.api.Option;


@Option.Group(NodeStatsTool.ID)
class NodeStatsCLI {
  @Option(name = "",
      help = "Enable NodeStatsTool.",
      category = OptionCategory.USER,
      stability = OptionStability.EXPERIMENTAL) //
  static final OptionKey<Boolean> ENABLED = new OptionKey<>(false);

  @Option(name = "OutputFile",
      help = "Save output to the given file.",
      category = OptionCategory.USER,
      stability = OptionStability.EXPERIMENTAL) //
  static final OptionKey<String> OUTPUT_FILE = new OptionKey<>("node-stats.yml");

  @Option(name = "Height",
      help = "The max. number of levels/height of the tree of a candidate.",
      category = OptionCategory.USER,
      stability = OptionStability.EXPERIMENTAL) //
  static final OptionKey<Integer> HEIGHT = new OptionKey<>(5);

  @Option(name = "Tracing",
      help = "Trace execution in addition to taking statistics",
      category = OptionCategory.USER,
      stability = OptionStability.EXPERIMENTAL) //
  static final OptionKey<Boolean> TRACING = new OptionKey<>(false);
}
