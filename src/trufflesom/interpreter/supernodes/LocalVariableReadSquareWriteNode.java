package trufflesom.interpreter.supernodes;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import bd.inlining.ScopeAdaptationVisitor;
import bd.inlining.ScopeAdaptationVisitor.ScopeElement;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.nodes.LocalVariableNode;


public abstract class LocalVariableReadSquareWriteNode extends LocalVariableNode {

  protected final Local readLocal;
  protected final int   readIndex;

  public LocalVariableReadSquareWriteNode(final Local writeLocal, final Local readLocal) {
    super(writeLocal);
    this.readLocal = readLocal;
    this.readIndex = readLocal.getIndex();
  }

  @Specialization(guards = {"isLongKind(frame)", "frame.isLong(readIndex)"},
      rewriteOn = {FrameSlotTypeException.class})
  public final long writeLong(final VirtualFrame frame) throws FrameSlotTypeException {
    long current = frame.getLong(readIndex);
    long result = Math.multiplyExact(current, current);
    frame.setLong(slotIndex, result);
    return result;
  }

  @Specialization(guards = {"isDoubleKind(frame)", "frame.isDouble(readIndex)"},
      rewriteOn = {FrameSlotTypeException.class})
  public final double writeDouble(final VirtualFrame frame) throws FrameSlotTypeException {
    double current = frame.getDouble(readIndex);
    double result = current * current;
    frame.setDouble(slotIndex, result);
    return result;
  }

  // uses frame to make sure guard is not converted to assertion
  protected final boolean isLongKind(final VirtualFrame frame) {
    FrameDescriptor descriptor = local.getFrameDescriptor();
    if (descriptor.getSlotKind(slotIndex) == FrameSlotKind.Long) {
      return true;
    }
    if (descriptor.getSlotKind(slotIndex) == FrameSlotKind.Illegal) {
      descriptor.setSlotKind(slotIndex, FrameSlotKind.Long);
      return true;
    }
    return false;
  }

  // uses frame to make sure guard is not converted to assertion
  protected final boolean isDoubleKind(final VirtualFrame frame) {
    FrameDescriptor descriptor = local.getFrameDescriptor();
    if (descriptor.getSlotKind(slotIndex) == FrameSlotKind.Double) {
      return true;
    }
    if (descriptor.getSlotKind(slotIndex) == FrameSlotKind.Illegal) {
      descriptor.setSlotKind(slotIndex, FrameSlotKind.Double);
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
