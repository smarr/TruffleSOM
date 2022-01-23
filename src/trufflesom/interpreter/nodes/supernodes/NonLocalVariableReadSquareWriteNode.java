package trufflesom.interpreter.nodes.supernodes;

import static trufflesom.interpreter.TruffleCompiler.transferToInterpreter;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import bd.inlining.ScopeAdaptationVisitor;
import bd.inlining.ScopeAdaptationVisitor.ScopeElement;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.nodes.NonLocalVariableNode;


public abstract class NonLocalVariableReadSquareWriteNode extends NonLocalVariableNode {

  protected final Local     readLocal;
  protected final FrameSlot readSlot;

  public NonLocalVariableReadSquareWriteNode(final int contextLevel, final Local writeLocal,
      final Local readLocal) {
    super(contextLevel, writeLocal);
    this.readLocal = readLocal;
    this.readSlot = readLocal.getSlot();
  }

  @Specialization(guards = {"isLongKind(ctx)", "ctx.isLong(readSlot)"},
      rewriteOn = {FrameSlotTypeException.class})
  public final long writeLong(final VirtualFrame frame,
      @Bind("determineContext(frame)") final MaterializedFrame ctx)
      throws FrameSlotTypeException {
    long current = ctx.getLong(readSlot);
    long result = Math.multiplyExact(current, current);

    ctx.setLong(slot, result);

    return result;
  }

  @Specialization(guards = {"isDoubleKind(ctx)", "ctx.isDouble(readSlot)"},
      rewriteOn = {FrameSlotTypeException.class})
  public final double writeDouble(final VirtualFrame frame,
      @Bind("determineContext(frame)") final MaterializedFrame ctx)
      throws FrameSlotTypeException {
    double current = ctx.getDouble(readSlot);
    double result = current * current;

    ctx.setDouble(slot, result);
    return result;
  }

  protected final boolean isLongKind(final VirtualFrame frame) {
    if (descriptor.getFrameSlotKind(slot) == FrameSlotKind.Long) {
      return true;
    }
    if (descriptor.getFrameSlotKind(slot) == FrameSlotKind.Illegal) {
      transferToInterpreter("LocalVar.writeIntToUninit");
      descriptor.setFrameSlotKind(slot, FrameSlotKind.Long);
      return true;
    }
    return false;
  }

  protected final boolean isDoubleKind(final VirtualFrame frame) {
    if (descriptor.getFrameSlotKind(slot) == FrameSlotKind.Double) {
      return true;
    }
    if (descriptor.getFrameSlotKind(slot) == FrameSlotKind.Illegal) {
      transferToInterpreter("LocalVar.writeDoubleToUninit");
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
