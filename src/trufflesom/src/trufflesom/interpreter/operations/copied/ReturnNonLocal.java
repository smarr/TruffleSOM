package trufflesom.interpreter.operations.copied;

import com.oracle.truffle.api.bytecode.OperationProxy.Proxyable;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;

import trufflesom.interpreter.FrameOnStackMarker;
import trufflesom.interpreter.ReturnException;
import trufflesom.interpreter.nodes.ContextualNode;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.vmobjects.SAbstractObject;
import trufflesom.vmobjects.SBlock;


@Proxyable
@NodeChild(value = "result", type = ExpressionNode.class)
@NodeChild(value = "onStackMarkerIndex", type = ExpressionNode.class)
@NodeChild(value = "contextLevel", type = ExpressionNode.class)
public abstract class ReturnNonLocal extends Node {

  public abstract Object executeGeneric(VirtualFrame frame);

  public abstract Object executeEvaluated(VirtualFrame frame, Object result,
      int onStackMarkerIndex, int contextLevel);

  @Specialization
  public static Object doReturn(final VirtualFrame frame, final Object result,
      final int onStackMarkerIndex, final int contextLevel,
      @Cached final InlinedBranchProfile blockEscaped,
      @Bind final Node selfNode) {
    MaterializedFrame ctx = ContextualNode.determineContext(frame, contextLevel);
    FrameOnStackMarker marker =
        (FrameOnStackMarker) ctx.getObject(onStackMarkerIndex);

    if (marker.isOnStack()) {
      throw new ReturnException(result, marker);
    } else {
      blockEscaped.enter(selfNode);
      SBlock block = (SBlock) frame.getArguments()[0];
      Object self = ctx.getArguments()[0];
      return SAbstractObject.sendEscapedBlock(self, block);
    }
  }
}
