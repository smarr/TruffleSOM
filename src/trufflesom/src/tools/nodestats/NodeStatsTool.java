package tools.nodestats;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.graalvm.options.OptionDescriptors;

import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags.RootTag;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Registration;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

import tools.nodestats.Tags.AnyNode;


/**
 * The {@link NodeStatsTool} is a Truffle instrumentation tool that collects statistics
 * about all {@link RootNode}s at the end of an execution.
 */
@Registration(name = "AST Node Statistics", id = NodeStatsTool.ID, version = "0.1",
    services = {NodeStatsTool.class})
public class NodeStatsTool extends TruffleInstrument {

  public static final String ID = "nodestats";

  private final Set<RootNode>             rootNodes;
  private final Map<Node, NodeActivation> nodeActivations;

  public NodeStatsTool() {
    rootNodes = new HashSet<>();
    nodeActivations = new HashMap<>();
  }

  @Override
  protected void onCreate(final Env env) {
    if (env.getOptions().get(NodeStatsCLI.ENABLED)) {
      Instrumenter instrumenter = env.getInstrumenter();

      collectRootNodesOnSourceLoad(instrumenter);
      instrumentNodesToCountActivations(instrumenter,
          env.getOptions().get(NodeStatsCLI.TRACING));
    }

    env.registerService(this);
  }

  private void collectRootNodesOnSourceLoad(final Instrumenter instrumenter) {
    SourceSectionFilter forRootNodes =
        SourceSectionFilter.newBuilder().tagIs(RootTag.class).build();

    instrumenter.attachLoadSourceSectionListener(
        forRootNodes, e -> rootNodes.add(e.getNode().getRootNode()),
        true);
  }

  private void instrumentNodesToCountActivations(final Instrumenter instrumenter,
      final boolean tracing) {
    SourceSectionFilter forAnyNode =
        SourceSectionFilter.newBuilder().tagIs(AnyNode.class).build();

    ExecutionEventNodeFactory factory;

    if (tracing) {
      factory = (final EventContext ctx) -> {
        Node instrumentedNode = ctx.getInstrumentedNode();
        NodeActivation node = new TracingNodeActivation(instrumentedNode);
        node.old = nodeActivations.get(instrumentedNode);
        nodeActivations.put(instrumentedNode, node);
        return node;
      };
    } else {
      factory = (final EventContext ctx) -> {
        Node instrumentedNode = ctx.getInstrumentedNode();
        NodeActivation node = new NodeActivation();
        node.old = nodeActivations.get(instrumentedNode);
        nodeActivations.put(instrumentedNode, node);
        return node;
      };
    }
    instrumenter.attachExecutionEventFactory(forAnyNode, factory);
  }

  @Override
  protected void onDispose(final Env env) {
    if (env.getOptions().get(NodeStatsCLI.ENABLED)) {
      String outputFile = env.getOptions().get(NodeStatsCLI.OUTPUT_FILE);
      int height = env.getOptions().get(NodeStatsCLI.HEIGHT);
      collectStatistics(outputFile, height);
    }
  }

  @Override
  protected OptionDescriptors getOptionDescriptors() {
    return new NodeStatsCLIOptionDescriptors();
  }

  private void collectStatistics(final String outputFile, final int height) {
    println("[ns] AST Node Statistics");
    println("[ns] -------------------\n");

    NodeStatisticsCollector collector = new NodeStatisticsCollector(height, nodeActivations);
    collector.addAll(rootNodes);
    collector.collectStats();

    println("[ns] Output File:          " + outputFile);
    println("[ns] Candidate Height:     " + height);
    println("[ns] Number of Methods:    " + rootNodes.size());
    println("[ns] Number of Nodes:      " + collector.getNumberOfNodes());
    println("[ns] Number of Node Types: " + collector.getNumberOfNodeTypes());
    println("[ns] All Activations: " + TracingNodeActivation.allActivations);

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
