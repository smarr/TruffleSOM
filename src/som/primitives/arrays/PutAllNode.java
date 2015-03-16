package som.primitives.arrays;

import java.util.Arrays;

import som.interpreter.Invokable;
import som.interpreter.nodes.dispatch.AbstractDispatchNode;
import som.interpreter.nodes.dispatch.UninitializedValuePrimDispatchNode;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.primitives.BlockPrims.ValuePrimitiveNode;
import som.vmobjects.SArray;
import som.vmobjects.SBlock;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

public abstract class PutAllNode extends BinaryExpressionNode
  implements ValuePrimitiveNode  {
  @Child private AbstractDispatchNode block;

  public PutAllNode() {
    super(null);
    block = new UninitializedValuePrimDispatchNode();
  }

  @Override
  public void adoptNewDispatchListHead(final AbstractDispatchNode node) {
    block = insert(node);
  }

  protected final static boolean notABlock(final Object[] rcvr,
      final Object value) {
    return !(value instanceof SBlock);
  }

  @Specialization
  public Object[] doPutEvalBlock(final VirtualFrame frame, final Object[] rcvr,
      final SBlock block) {
    try {
      if (SArray.FIRST_IDX < rcvr.length) {
        rcvr[SArray.FIRST_IDX] = this.block.executeDispatch(frame, new Object[] {block});
      }
      for (int i = SArray.FIRST_IDX + 1; i < rcvr.length; i++) {
        rcvr[i] = this.block.executeDispatch(frame, new Object[] {block});
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount(rcvr.length);
      }
    }
    return rcvr;
  }

  protected final void reportLoopCount(final long count) {
    if (count == 0) {
      return;
    }

    CompilerAsserts.neverPartOfCompilation("reportLoopCount");
    Node current = getParent();
    while (current != null && !(current instanceof RootNode)) {
      current = current.getParent();
    }
    if (current != null) {
      ((Invokable) current).propagateLoopCountThroughoutLexicalScope(count);
    }
  }

  @Specialization(guards = {"notABlock"})
  public Object[] doPutObject(final Object[] rcvr, final Object value) {
    Arrays.fill(rcvr, value);
    return rcvr;
  }
}
