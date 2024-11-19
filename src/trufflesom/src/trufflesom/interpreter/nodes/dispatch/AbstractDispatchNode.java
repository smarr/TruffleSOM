package trufflesom.interpreter.nodes.dispatch;

import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.bdt.inlining.nodes.WithSource;
import trufflesom.bdt.source.SourceCoordinate;
import trufflesom.interpreter.nodes.AbstractMessageSendNode;
import trufflesom.tools.nodestats.Tags.AnyNode;
import trufflesom.vm.VmSettings;
import trufflesom.vmobjects.SSymbol;


@GenerateWrapper
public abstract class AbstractDispatchNode extends Node
    implements DispatchChain, InstrumentableNode, WithSource {
  public static final int INLINE_CACHE_SIZE = 6;

  @NeverDefault
  public static AbstractDispatchNode create(final Object selector) {
    return new UninitializedDispatchNode((SSymbol) selector);
  }

  public abstract Object executeDispatch(VirtualFrame frame, Object[] arguments);

  @Override
  public boolean isInstrumentable() {
    return true;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Node> T initialize(final long sourceCoord) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getSourceCoordinate() {
    Node node = getSendNode();
    if (node instanceof AbstractMessageSendNode send) {
      return send.getSourceCoordinate();
    } else {
      return 0;
    }
  }

  @Override
  public Source getSource() {
    Node node = getSendNode();
    if (node instanceof AbstractMessageSendNode send) {
      return send.getSource();
    } else {
      SourceSection ss = node.getSourceSection();
      if (ss == null) {
        return null;
      }
      return ss.getSource();
    }
  }

  @Override
  public boolean hasSource() {
    return false;
  }

  @Override
  public WrapperNode createWrapper(final ProbeNode probe) {
    return new AbstractDispatchNodeWrapper(this, probe);
  }

  @Override
  public boolean hasTag(final Class<? extends Tag> tag) {
    if (tag == AnyNode.class) {
      return true;
    }
    return false;
  }

  public void notifyAsInserted() {
    if (VmSettings.UseInstrumentation) {
      notifyInserted(this);
    }
  }

  @Override
  public SourceSection getSourceSection() {
    Node node = getSendNode();
    if (node instanceof AbstractMessageSendNode send) {
      return SourceCoordinate.createSourceSection(send, send.getSourceCoordinate());
    } else {
      return node.getSourceSection();
    }
  }

  protected Node getSendNode() {
    Node i = this;
    while (i.getParent() instanceof AbstractDispatchNode) {
      i = i.getParent();
    }
    return i.getParent();
  }
}
