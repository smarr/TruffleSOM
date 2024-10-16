package trufflesom.interpreter.supernodes.inc;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import trufflesom.bdt.inlining.ScopeAdaptationVisitor;
import trufflesom.bdt.inlining.ScopeAdaptationVisitor.ScopeElement;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.Method.OpBuilder;
import trufflesom.interpreter.nodes.LocalVariableNode;


public abstract class IncLocalVarWithValueNode extends LocalVariableNode {
  private final long incValue;

  public IncLocalVarWithValueNode(final Local variable, final long incValue) {
    super(variable);
    this.incValue = incValue;
  }

  @Specialization(guards = "frame.isLong(slotIndex)",
      rewriteOn = {FrameSlotTypeException.class})
  public final long doLong(final VirtualFrame frame) throws FrameSlotTypeException {
    long current = frame.getLong(slotIndex);
    long result = Math.addExact(current, incValue);
    frame.setLong(slotIndex, result);
    return result;
  }

  @Specialization(guards = "frame.isDouble(slotIndex)",
      rewriteOn = {FrameSlotTypeException.class})
  public final double doDouble(final VirtualFrame frame) throws FrameSlotTypeException {
    double current = frame.getDouble(slotIndex);
    double result = current + incValue;
    frame.setDouble(slotIndex, result);
    return result;
  }

  @Override
  public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
    ScopeElement se = inliner.getAdaptedVar(local);
    if (se.var != local || se.contextLevel < 0) {
      replace(se.var.getIncNode(se.contextLevel, incValue, sourceCoord));
    } else {
      assert 0 == se.contextLevel;
    }
  }

  @Override
  public void constructOperation(final OpBuilder opBuilder, boolean resultUsed) {
    opBuilder.dsl.emitIncLocalVarWithValue(opBuilder.getLocal(local), incValue);
  }
}
