package trufflesom.interpreter.supernodes;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import bdt.inlining.ScopeAdaptationVisitor;
import bdt.inlining.ScopeAdaptationVisitor.ScopeElement;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.nodes.LocalVariableNode;


public abstract class IntIncLocalVariableNode extends LocalVariableNode {

  private final long incValue;

  public IntIncLocalVariableNode(final Local variable, final long incValue) {
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
    ScopeElement<? extends Node> se = inliner.getAdaptedVar(local);
    if (se.var != local || se.contextLevel < 0) {
      replace(se.var.getIncNode(se.contextLevel, incValue, sourceCoord));
    } else {
      assert 0 == se.contextLevel;
    }
  }
}
