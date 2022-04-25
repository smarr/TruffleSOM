package trufflesom.interpreter.supernodes;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import bdt.inlining.ScopeAdaptationVisitor;
import bdt.inlining.ScopeAdaptationVisitor.ScopeElement;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.nodes.NonLocalVariableNode;


public abstract class NonLocalVariableReadSquareWriteNode extends NonLocalVariableNode {

  protected final Local readLocal;
  protected final int   readIndex;

  public NonLocalVariableReadSquareWriteNode(final int contextLevel, final Local writeLocal,
      final Local readLocal) {
    super(contextLevel, writeLocal);
    this.readLocal = readLocal;
    this.readIndex = readLocal.getIndex();
  }

  @Specialization(guards = {"isLongKind(ctx)", "ctx.isLong(readIndex)"},
      rewriteOn = {FrameSlotTypeException.class})
  public final long writeLong(final VirtualFrame frame,
      @Bind("determineContext(frame)") final MaterializedFrame ctx)
      throws FrameSlotTypeException {
    long current = ctx.getLong(readIndex);
    long result = Math.multiplyExact(current, current);

    ctx.setLong(slotIndex, result);

    return result;
  }

  @Specialization(guards = {"isDoubleKind(ctx)", "ctx.isDouble(readIndex)"},
      rewriteOn = {FrameSlotTypeException.class})
  public final double writeDouble(final VirtualFrame frame,
      @Bind("determineContext(frame)") final MaterializedFrame ctx)
      throws FrameSlotTypeException {
    double current = ctx.getDouble(readIndex);
    double result = current * current;

    ctx.setDouble(slotIndex, result);
    return result;
  }

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

    if (seWrite.var != local || seWrite.contextLevel < contextLevel) {
      assert seRead.var != readLocal || seRead.contextLevel < contextLevel;
      replace(
          seWrite.var.getReadSquareWriteNode(seWrite.contextLevel, sourceCoord,
              (Local) seRead.var));
    } else {
      assert contextLevel == seWrite.contextLevel;
    }
  }
}
