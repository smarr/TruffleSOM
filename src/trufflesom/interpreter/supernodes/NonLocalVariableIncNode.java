package trufflesom.interpreter.supernodes;

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


public abstract class NonLocalVariableIncNode extends NonLocalVariableNode {

  private final long incValue;

  public NonLocalVariableIncNode(final int contextLevel, final Local local,
      final long incValue) {
    super(contextLevel, local);
    this.incValue = incValue;
  }

  @Specialization(guards = "ctx.isLong(slot)", rewriteOn = {FrameSlotTypeException.class})
  public final long doLong(final VirtualFrame frame,
      @Bind("determineContext(frame)") final MaterializedFrame ctx)
      throws FrameSlotTypeException {
    long current = ctx.getLong(slot);
    long result = Math.addExact(current, incValue);
    ctx.setLong(slot, result);
    return result;
  }

  @Specialization(guards = "ctx.isDouble(slot)", rewriteOn = {FrameSlotTypeException.class})
  public final double doDouble(final VirtualFrame frame,
      @Bind("determineContext(frame)") final MaterializedFrame ctx)
      throws FrameSlotTypeException {
    double current = ctx.getDouble(slot);
    double result = current + incValue;
    ctx.setDouble(slot, result);
    return result;
  }

  @Override
  public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
    ScopeElement<? extends Node> se = inliner.getAdaptedVar(local);
    if (se.var != local || se.contextLevel < contextLevel) {
      replace(se.var.getIncNode(se.contextLevel, incValue, sourceCoord));
    } else {
      assert contextLevel == se.contextLevel;
    }
  }
}
