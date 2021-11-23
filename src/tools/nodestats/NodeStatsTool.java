package tools.nodestats;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.graalvm.options.OptionDescriptors;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Instrument;

import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags.RootTag;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Registration;
import com.oracle.truffle.api.nodes.RootNode;

/**
 * The {@link NodeStatsTool} is a Truffle instrumentation tool that collects statistics about all
 * {@link RootNode}s at the end of an execution.
 */
@Registration(name = "AST Node Statistics", id = NodeStatsTool.ID, version = "0.1", services = {NodeStatsTool.class})
public class NodeStatsTool extends TruffleInstrument {

    public static final String ID = "nodestats";

    private final Set<RootNode> rootNodes;

    public NodeStatsTool() {
        rootNodes = new HashSet<>();
    }

    public static void enable(final Engine engine) {
        Instrument instrument = engine.getInstruments().get(ID);
        if (instrument == null) {
            throw new IllegalStateException(
                            "NodeStatsTool not properly installed into polyglot.Engine");
        }
        instrument.lookup(NodeStatsTool.class);
    }

    @Override
    protected void onCreate(final Env env) {
        if (env.getOptions().get(NodeStatsCLI.ENABLED)) {
            Instrumenter instrumenter = env.getInstrumenter();

            SourceSectionFilter rootFilter = SourceSectionFilter.newBuilder().tagIs(RootTag.class).build();

            instrumenter.attachLoadSourceSectionListener(
                            rootFilter, e -> rootNodes.add(e.getNode().getRootNode()),
                            true);
        }

        env.registerService(this);
    }

    @Override
    protected void onDispose(final Env env) {
        if (env.getOptions().get(NodeStatsCLI.ENABLED)) {
            String outputFile = System.getProperty("ns.output", "node-stats.yml");
            collectStatistics(outputFile);
        }
    }

    @Override
    protected OptionDescriptors getOptionDescriptors() {
        return new NodeStatsCLIOptionDescriptors();
    }

    private void collectStatistics(final String outputFile) {
        println("[ns] AST Node Statistics");
        println("[ns] -------------------\n");

        NodeStatisticsCollector collector = new NodeStatisticsCollector(3);
        collector.addAll(rootNodes);
        collector.collectStats();

        println("[ns] Output File:          " + outputFile);
        println("[ns] Number of Methods:    " + rootNodes.size());
        println("[ns] Number of Nodes:      " + collector.getNumberOfNodes());
        println("[ns] Number of Node Types: " + collector.getNumberOfNodeTypes());

        String report = YamlReport.createReport(collector);
        Path reportPath = Paths.get(outputFile);
        try {
            Files.write(reportPath, report.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Could not write AST Node Statistics: " + e);
        }
    }

    public static void println(final String msg) {
        // Checkstyle: stop
        System.out.println(msg);
        // Checkstyle: resume
    }
}
