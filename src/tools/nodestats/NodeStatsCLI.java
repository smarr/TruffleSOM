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
}
