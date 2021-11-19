package tools.nodestats;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Instrument;

import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags.RootTag;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Registration;
import com.oracle.truffle.api.nodes.RootNode;

import tools.nodestats.SubTree.ScoredAlphabeticRootOrder;


/**
 * The {@link NodeStatsTool} is a Truffle instrumentation tool that collects
 * statistics about all {@link RootNode}s at the end of an execution.
 */
@Registration(name = "AST Node Statistics", id = NodeStatsTool.ID, version = "0.1",
    services = {NodeStatsTool.class})
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
    Instrumenter instrumenter = env.getInstrumenter();

    SourceSectionFilter rootFilter =
        SourceSectionFilter.newBuilder().tagIs(RootTag.class).build();

    instrumenter.attachLoadSourceSectionListener(
        rootFilter, e -> rootNodes.add(e.getNode().getRootNode()),
        true);

    env.registerService(this);
  }

  @Override
  protected void onDispose(final Env env) {
    String outputFile = System.getProperty("ns.output", "node-stats.yml");
    collectStatistics(outputFile);
  }

  private void collectStatistics(final String outputFile) {
    println("[ns] AST Node Statistics");
    println("[ns] -------------------");
    println("[ns] Number of Methods: " + rootNodes.size());

    NodeStatisticsCollector collector = new NodeStatisticsCollector(3);
    collector.addAll(rootNodes);
    collector.collectCandidates();

    Set<SubTree> cs = collector.getSubTrees();
    cs.stream();

    final List<SubTree> sorted = cs.stream()
                                   .sorted(new ScoredAlphabeticRootOrder())
                                   .collect(Collectors.toList());

    // Universe.println("[ns] Number of Nodes: " + collector.getNumberOfAstNodes());

    StringBuilder builder = new StringBuilder();
    builder.append("# Static Node Frequency\n\n");

    for (SubTree c : sorted) {
      c.prettyPrint(builder);
      builder.append('\n');
    }

    Path reportPath = Paths.get(outputFile);
    try {
      Files.write(reportPath, builder.toString().getBytes());
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
