package trufflesom.interpreter.supernodes;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import bdt.inlining.ScopeAdaptationVisitor;
import bdt.inlining.ScopeAdaptationVisitor.ScopeElement;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.nodes.NonLocalVariableNode;


public abstract class NonLocalVariableSquareNode extends NonLocalVariableNode {

  public NonLocalVariableSquareNode(final int contextLevel, final Local local) {
    super(contextLevel, local);
  }

  @Specialization(guards = {"ctx.isLong(slotIndex)"},
      rewriteOn = {FrameSlotTypeException.class})
  public final long doLong(final VirtualFrame frame,
      @Bind("determineContext(frame)") final MaterializedFrame ctx)
      throws FrameSlotTypeException {
    long current = ctx.getLong(slotIndex);
    return Math.multiplyExact(current, current);
  }

  @Specialization(guards = {"ctx.isDouble(slotIndex)"},
      rewriteOn = {FrameSlotTypeException.class})
  public final double doDouble(final VirtualFrame frame,
      @Bind("determineContext(frame)") final MaterializedFrame ctx)
      throws FrameSlotTypeException {
    double current = ctx.getDouble(slotIndex);
    return current * current;
  }

  @Override
  public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
    ScopeElement<? extends Node> se = inliner.getAdaptedVar(local);
    if (se.var != local || se.contextLevel < contextLevel) {
      replace(se.var.getSquareNode(se.contextLevel, sourceCoord));
    } else {
      assert contextLevel == se.contextLevel;
    }
  }
}
