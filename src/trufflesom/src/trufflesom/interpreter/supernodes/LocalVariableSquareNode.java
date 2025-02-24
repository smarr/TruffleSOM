package trufflesom.interpreter.supernodes;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import trufflesom.bdt.inlining.ScopeAdaptationVisitor;
import trufflesom.bdt.inlining.ScopeAdaptationVisitor.ScopeElement;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.Method.OpBuilder;
import trufflesom.interpreter.nodes.LocalVariableNode;


public abstract class LocalVariableSquareNode extends LocalVariableNode {

  public LocalVariableSquareNode(final Local variable) {
    super(variable);
  }

  @Specialization(guards = {"frame.isLong(slotIndex)"},
      rewriteOn = {FrameSlotTypeException.class})
  public final long doLong(final VirtualFrame frame) throws FrameSlotTypeException {
    long value = frame.getLong(slotIndex);
    return Math.multiplyExact(value, value);
  }

  @Specialization(guards = {"frame.isDouble(slotIndex)"},
      rewriteOn = {FrameSlotTypeException.class})
  public final double doDouble(final VirtualFrame frame) throws FrameSlotTypeException {
    double value = frame.getDouble(slotIndex);
    return value * value;
  }

  @Override
  public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
    ScopeElement se = inliner.getAdaptedVar(local);
    if (se.var != local || se.contextLevel < 0) {
      replace(se.var.getSquareNode(se.contextLevel, sourceCoord));
    } else {
      assert 0 == se.contextLevel;
    }
  }

  @Override
  public void constructOperation(final OpBuilder opBuilder, boolean resultUsed) {
    opBuilder.dsl.beginSquare();
    opBuilder.dsl.emitLoadLocal(opBuilder.getLocal(local));
    opBuilder.dsl.endSquare();
  }
}
