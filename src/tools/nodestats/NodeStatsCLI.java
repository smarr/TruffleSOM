package tools.nodestats;

import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionKey;
import org.graalvm.options.OptionStability;

import com.oracle.truffle.api.Option;

@Option.Group(NodeStatsTool.ID)
class NodeStatsCLI {
    @Option(name = "", help = "Enable the CPU sampler.", category = OptionCategory.USER, stability = OptionStability.STABLE) //
    static final OptionKey<Boolean> ENABLED = new OptionKey<>(false);
}
