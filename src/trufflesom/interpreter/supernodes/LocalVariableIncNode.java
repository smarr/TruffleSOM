package trufflesom.interpreter.supernodes;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import bd.inlining.ScopeAdaptationVisitor;
import bd.inlining.ScopeAdaptationVisitor.ScopeElement;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.nodes.LocalVariableNode;


public abstract class LocalVariableIncNode extends LocalVariableNode {

  private final long incValue;

  public LocalVariableIncNode(final Local variable, final long incValue) {
    super(variable);
    this.incValue = incValue;
  }

  @Specialization(guards = "frame.isLong(slot)", rewriteOn = {FrameSlotTypeException.class})
  public final long doLong(final VirtualFrame frame) throws FrameSlotTypeException {
    long current = frame.getLong(slot);
    long result = Math.addExact(current, incValue);
    frame.setLong(slot, result);
    return result;
  }

  @Specialization(guards = "frame.isDouble(slot)",
      rewriteOn = {FrameSlotTypeException.class})
  public final double doDouble(final VirtualFrame frame) throws FrameSlotTypeException {
    double current = frame.getDouble(slot);
    double result = current + incValue;
    frame.setDouble(slot, result);
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
