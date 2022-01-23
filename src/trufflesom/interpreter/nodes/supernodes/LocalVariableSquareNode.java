package trufflesom.interpreter.nodes.supernodes;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import bd.inlining.ScopeAdaptationVisitor;
import bd.inlining.ScopeAdaptationVisitor.ScopeElement;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.nodes.LocalVariableNode;


public abstract class LocalVariableSquareNode extends LocalVariableNode {

  public LocalVariableSquareNode(final Local variable) {
    super(variable);
  }

  @Specialization(guards = {"frame.isLong(slot)"}, rewriteOn = {FrameSlotTypeException.class})
  public final long doLong(final VirtualFrame frame) throws FrameSlotTypeException {
    long value = frame.getLong(slot);
    return Math.multiplyExact(value, value);
  }

  @Specialization(guards = {"frame.isDouble(slot)"},
      rewriteOn = {FrameSlotTypeException.class})
  public final double doDouble(final VirtualFrame frame) throws FrameSlotTypeException {
    double value = frame.getDouble(slot);
    return value * value;
  }

  @Override
  public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
    ScopeElement<? extends Node> se = inliner.getAdaptedVar(local);
    if (se.var != local || se.contextLevel < 0) {
      replace(se.var.getSquareNode(se.contextLevel, sourceCoord));
    } else {
      assert 0 == se.contextLevel;
    }
  }
}
