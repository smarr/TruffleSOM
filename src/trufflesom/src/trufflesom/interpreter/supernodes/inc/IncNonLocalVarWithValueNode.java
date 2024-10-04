package trufflesom.interpreter.supernodes.inc;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.bdt.inlining.ScopeAdaptationVisitor;
import trufflesom.bdt.inlining.ScopeAdaptationVisitor.ScopeElement;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.NonLocalVariableNode;


public abstract class IncNonLocalVarWithValueNode extends NonLocalVariableNode {
  protected final long incValue;

  public IncNonLocalVarWithValueNode(final int contextLevel, final Local local,
      final long incValue) {
    super(contextLevel, local);
    this.incValue = incValue;
  }

  @Specialization(guards = "ctx.isLong(slotIndex)", rewriteOn = {FrameSlotTypeException.class})
  public final long doLong(final VirtualFrame frame,
      @Bind("determineContext(frame)") final MaterializedFrame ctx)
      throws FrameSlotTypeException {
    long current = ctx.getLong(slotIndex);
    long result = Math.addExact(current, incValue);
    ctx.setLong(slotIndex, result);
    return result;
  }

  @Specialization(guards = "ctx.isDouble(slotIndex)",
      rewriteOn = {FrameSlotTypeException.class})
  public final double doDouble(final VirtualFrame frame,
      @Bind("determineContext(frame)") final MaterializedFrame ctx)
      throws FrameSlotTypeException {
    double current = ctx.getDouble(slotIndex);
    double result = current + incValue;
    ctx.setDouble(slotIndex, result);
    return result;
  }

  @Override
  public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
    ScopeElement se = inliner.getAdaptedVar(local);
    if (se.var != local || se.contextLevel < contextLevel) {
      replace(se.var.getIncNode(se.contextLevel, incValue, sourceCoord));
    } else {
      assert contextLevel == se.contextLevel;
    }
  }
}
