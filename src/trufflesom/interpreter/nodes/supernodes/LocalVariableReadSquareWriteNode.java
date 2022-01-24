package trufflesom.interpreter.nodes.supernodes;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import bd.inlining.ScopeAdaptationVisitor;
import bd.inlining.ScopeAdaptationVisitor.ScopeElement;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.nodes.LocalVariableNode;


public abstract class LocalVariableReadSquareWriteNode extends LocalVariableNode {

  protected final Local     readLocal;
  protected final FrameSlot readSlot;

  public LocalVariableReadSquareWriteNode(final Local writeLocal, final Local readLocal) {
    super(writeLocal);
    this.readLocal = readLocal;
    this.readSlot = readLocal.getSlot();
  }

  @Specialization(guards = {"isLongKind(frame)", "frame.isLong(readSlot)"},
      rewriteOn = {FrameSlotTypeException.class})
  public final long writeLong(final VirtualFrame frame) throws FrameSlotTypeException {
    long current = frame.getLong(readSlot);
    long result = Math.multiplyExact(current, current);
    frame.setLong(slot, result);
    return result;
  }

  @Specialization(guards = {"isDoubleKind(frame)", "frame.isDouble(readSlot)"},
      rewriteOn = {FrameSlotTypeException.class})
  public final double writeDouble(final VirtualFrame frame) throws FrameSlotTypeException {
    double current = frame.getDouble(readSlot);
    double result = current * current;
    frame.setDouble(slot, result);
    return result;
  }

  // uses frame to make sure guard is not converted to assertion
  protected final boolean isLongKind(final VirtualFrame frame) {
    if (descriptor.getFrameSlotKind(slot) == FrameSlotKind.Long) {
      return true;
    }
    if (descriptor.getFrameSlotKind(slot) == FrameSlotKind.Illegal) {
      descriptor.setFrameSlotKind(slot, FrameSlotKind.Long);
      return true;
    }
    return false;
  }

  // uses frame to make sure guard is not converted to assertion
  protected final boolean isDoubleKind(final VirtualFrame frame) {
    if (descriptor.getFrameSlotKind(slot) == FrameSlotKind.Double) {
      return true;
    }
    if (descriptor.getFrameSlotKind(slot) == FrameSlotKind.Illegal) {
      descriptor.setFrameSlotKind(slot, FrameSlotKind.Double);
      return true;
    }
    return false;
  }

  @Override
  public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
    ScopeElement<? extends Node> seWrite = inliner.getAdaptedVar(local);
    ScopeElement<? extends Node> seRead = inliner.getAdaptedVar(readLocal);

    assert seWrite.contextLevel == seRead.contextLevel;

    if (seWrite.var != local || seWrite.contextLevel < 0) {
      assert seRead.var != readLocal || seRead.contextLevel < 0;
      replace(
          seWrite.var.getReadSquareWriteNode(seWrite.contextLevel, sourceCoord,
              (Local) seRead.var));
    } else {
      assert 0 == seWrite.contextLevel;
    }
  }
}
