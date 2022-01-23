package trufflesom.interpreter.nodes.supernodes;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import bd.inlining.ScopeAdaptationVisitor;
import bd.inlining.ScopeAdaptationVisitor.ScopeElement;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.nodes.NonLocalVariableNode;


public abstract class NonLocalVariableSquareNode extends NonLocalVariableNode {

  public NonLocalVariableSquareNode(final int contextLevel, final Local local) {
    super(contextLevel, local);
  }

  @Specialization(guards = {"ctx.isLong(slot)"}, rewriteOn = {FrameSlotTypeException.class})
  public final long doLong(final VirtualFrame frame,
      @Bind("determineContext(frame)") final MaterializedFrame ctx)
      throws FrameSlotTypeException {
    long current = ctx.getLong(slot);
    return current * current;
  }

  @Specialization(guards = {"ctx.isDouble(slot)"}, rewriteOn = {FrameSlotTypeException.class})
  public final double doDouble(final VirtualFrame frame,
      @Bind("determineContext(frame)") final MaterializedFrame ctx)
      throws FrameSlotTypeException {
    double current = ctx.getDouble(slot);
    return current * current;
  }

  @Override
  public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
    ScopeElement<? extends Node> se = inliner.getAdaptedVar(local);
    if (se.var != local || se.contextLevel < contextLevel) {
      replace(se.var.getReadNode(se.contextLevel, sourceCoord));
    } else {
      assert contextLevel == se.contextLevel;
    }
  }
}
