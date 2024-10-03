package trufflesom.interpreter.supernodes;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import trufflesom.bdt.inlining.ScopeAdaptationVisitor;
import trufflesom.bdt.inlining.ScopeAdaptationVisitor.ScopeElement;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.nodes.NonLocalVariableNode;
import trufflesom.vmobjects.SBlock;


public abstract class NonLocalVariableReadSquareWriteNode extends NonLocalVariableNode {

  protected final Local readLocal;
  protected final int   readIndex;
  protected final int   readContextLevel;

  public NonLocalVariableReadSquareWriteNode(final int writeContextLevel,
      final Local writeLocal,
      final Local readLocal, final int readContextLevel) {
    super(writeContextLevel, writeLocal);
    this.readLocal = readLocal;
    this.readIndex = readLocal.getIndex();
    this.readContextLevel = readContextLevel;
  }

  @ExplodeLoop
  protected final Frame determineReadContext(final VirtualFrame frame) {
    if (readContextLevel == 0) {
      return frame;
    }

    SBlock self = (SBlock) frame.getArguments()[0];
    int i = readContextLevel - 1;

    while (i > 0) {
      self = (SBlock) self.getOuterSelf();
      i--;
    }

    // Graal needs help here to see that this is always a MaterializedFrame
    // so, we record explicitly a class profile
    return frameType.profile(self.getContext());
  }

  @Specialization(
      guards = {"isLongKind(ctx)", "ctx.isLong(readIndex)",
          "contextLevel == readContextLevel"},
      rewriteOn = {FrameSlotTypeException.class})
  public final long writeLongSameContext(final VirtualFrame frame,
      @Bind("determineContext(frame)") final MaterializedFrame ctx)
      throws FrameSlotTypeException {
    long current = ctx.getLong(readIndex);
    long result = Math.multiplyExact(current, current);

    ctx.setLong(slotIndex, result);

    return result;
  }

  @Specialization(guards = {"isLongKind(writeCtx)", "readCtx.isLong(readIndex)"},
      rewriteOn = {FrameSlotTypeException.class})
  public final long writeLong(final VirtualFrame frame,
      @Bind("determineContext(frame)") final MaterializedFrame writeCtx,
      @Bind("determineReadContext(frame)") final Frame readCtx)
      throws FrameSlotTypeException {
    long current = readCtx.getLong(readIndex);
    long result = Math.multiplyExact(current, current);

    writeCtx.setLong(slotIndex, result);

    return result;
  }

  @Specialization(
      guards = {"isDoubleKind(ctx)", "ctx.isDouble(readIndex)",
          "contextLevel == readContextLevel"},
      rewriteOn = {FrameSlotTypeException.class})
  public final double writeDoubleSameContext(final VirtualFrame frame,
      @Bind("determineContext(frame)") final MaterializedFrame ctx)
      throws FrameSlotTypeException {
    double current = ctx.getDouble(readIndex);
    double result = current * current;

    ctx.setDouble(slotIndex, result);
    return result;
  }

  @Specialization(guards = {"isDoubleKind(writeCtx)", "readCtx.isDouble(readIndex)"},
      rewriteOn = {FrameSlotTypeException.class})
  public final double writeDouble(final VirtualFrame frame,
      @Bind("determineContext(frame)") final MaterializedFrame writeCtx,
      @Bind("determineReadContext(frame)") final Frame readCtx)
      throws FrameSlotTypeException {
    double current = readCtx.getDouble(readIndex);
    double result = current * current;

    writeCtx.setDouble(slotIndex, result);
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
    ScopeElement seWrite = inliner.getAdaptedVar(local);
    ScopeElement seRead = inliner.getAdaptedVar(readLocal);

    assert seWrite.contextLevel == seRead.contextLevel;

    if (seWrite.var != local || seWrite.contextLevel < contextLevel) {
      assert seRead.var != readLocal || seRead.contextLevel < contextLevel;
      replace(seWrite.var.getReadSquareWriteNode(seWrite.contextLevel, sourceCoord,
          (Local) seRead.var, seRead.contextLevel));
    } else {
      assert contextLevel == seWrite.contextLevel;
    }
  }
}
