package som.interpreter;

import som.interpreter.nodes.ExpressionNode;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;

public abstract class Invokable extends RootNode {
  protected final FrameSlot frameOnStackMarker;

  @Child protected ExpressionNode  expressionOrSequence;

  public Invokable(final SourceSection sourceSection,
      final FrameDescriptor frameDescriptor,
      final FrameSlot frameOnStackMarker,
      final ExpressionNode expressionOrSequence) {
    super(sourceSection, frameDescriptor);
    this.expressionOrSequence = expressionOrSequence;
    this.frameOnStackMarker   = frameOnStackMarker;
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
        if (!e.reachedTarget(marker)) {
          marker.frameNoLongerOnStack();
          throw e;
        } else {
          result = e.result();
        }
      }

      marker.frameNoLongerOnStack();
      return result;
  }

  @Override
  public final boolean isCloningAllowed() {
    return false;
  }

  public final RootCallTarget createCallTarget() {
    return Truffle.getRuntime().createCallTarget(this);
  }
}
