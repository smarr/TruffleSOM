package som.interpreter;

import som.interpreter.nodes.ExpressionNode;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;

public abstract class Invokable extends RootNode {
  private final BranchProfile nonLocalReturnHandler;
  private final BranchProfile doCatch;
  private final BranchProfile doPropagate;
  protected final FrameSlot frameOnStackMarker;

  @Child protected ExpressionNode  expressionOrSequence;

  private final ExpressionNode  uninitializedBody;

  public Invokable(final SourceSection sourceSection,
      final FrameDescriptor frameDescriptor,
      final FrameSlot frameOnStackMarker,
      final ExpressionNode expressionOrSequence) {
    super(sourceSection, frameDescriptor);
    this.uninitializedBody    = NodeUtil.cloneNode(expressionOrSequence);
    this.expressionOrSequence = expressionOrSequence;

    this.nonLocalReturnHandler = BranchProfile.create();
    this.frameOnStackMarker    = frameOnStackMarker;

    this.doCatch     = BranchProfile.create();
    this.doPropagate = BranchProfile.create();
  }

  public ExpressionNode getUninitializedBody() {
    return uninitializedBody;
  }

  @Override
  public final Object execute(final VirtualFrame frame) {
      FrameOnStackMarker marker = new FrameOnStackMarker();
      frameOnStackMarker.setKind(FrameSlotKind.Object);
      frame.setObject(frameOnStackMarker, marker);

      Object result;
      try {
        result = expressionOrSequence.executeGeneric(frame);
      } catch (ReturnException e) {
        nonLocalReturnHandler.enter();
        if (!e.reachedTarget(marker)) {
          doPropagate.enter();
          marker.frameNoLongerOnStack();
          throw e;
        } else {
          doCatch.enter();
          result = e.result();
        }
      }

      marker.frameNoLongerOnStack();
      return result;
  }

  public abstract Invokable cloneWithNewLexicalContext(final LexicalContext outerContext);

  @Override
  public final boolean isCloningAllowed() {
    return true;
  }

  public final RootCallTarget createCallTarget() {
    return Truffle.getRuntime().createCallTarget(this);
  }

  public abstract void propagateLoopCountThroughoutLexicalScope(final long count);

  public abstract boolean isBlock();
}
