package som.interpreter.nodes;

import static som.interpreter.TruffleCompiler.transferToInterpreter;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;

import som.interpreter.InlinerAdaptToEmbeddedOuterContext;
import som.interpreter.InlinerForLexicallyEmbeddedMethods;
import som.vm.constants.Nil;
import som.vmobjects.SObject;


public abstract class NonLocalVariableNode extends ContextualNode {

  protected final FrameSlot slot;

  private NonLocalVariableNode(final int contextLevel, final FrameSlot slot) {
    super(contextLevel);
    this.slot = slot;
  }

  @Override
  public final void replaceWithLexicallyEmbeddedNode(
      final InlinerForLexicallyEmbeddedMethods inliner) {
    throw new RuntimeException(
        "Normally, only uninitialized variable nodes should be encountered, because this is done at parse time");
  }

  @Override
  public final void replaceWithCopyAdaptedToEmbeddedOuterContext(
      final InlinerAdaptToEmbeddedOuterContext inliner) {
    throw new RuntimeException(
        "Normally, only uninitialized variable nodes should be encountered, because this is done at parse time");
  }

  public abstract static class NonLocalVariableReadNode extends NonLocalVariableNode {

    public NonLocalVariableReadNode(final int contextLevel, final FrameSlot slot) {
      super(contextLevel, slot);
    }

    @Specialization(guards = "isUninitialized(frame)")
    public final SObject doNil(final VirtualFrame frame) {
      return Nil.nilObject;
    }

    protected boolean isBoolean(final VirtualFrame frame) {
      return determineContext(frame).isBoolean(slot);
    }

    protected boolean isLong(final VirtualFrame frame) {
      return determineContext(frame).isLong(slot);
    }

    protected boolean isDouble(final VirtualFrame frame) {
      return determineContext(frame).isDouble(slot);
    }

    protected boolean isObject(final VirtualFrame frame) {
      return determineContext(frame).isObject(slot);
    }

    @Specialization(guards = {"isBoolean(frame)"}, rewriteOn = {FrameSlotTypeException.class})
    public final boolean doBoolean(final VirtualFrame frame) throws FrameSlotTypeException {
      return determineContext(frame).getBoolean(slot);
    }

    @Specialization(guards = {"isLong(frame)"}, rewriteOn = {FrameSlotTypeException.class})
    public final long doLong(final VirtualFrame frame) throws FrameSlotTypeException {
      return determineContext(frame).getLong(slot);
    }

    @Specialization(guards = {"isDouble(frame)"}, rewriteOn = {FrameSlotTypeException.class})
    public final double doDouble(final VirtualFrame frame) throws FrameSlotTypeException {
      return determineContext(frame).getDouble(slot);
    }

    @Specialization(guards = {"isObject(frame)"},
        replaces = {"doBoolean", "doLong", "doDouble"},
        rewriteOn = {FrameSlotTypeException.class})
    public final Object doObject(final VirtualFrame frame) throws FrameSlotTypeException {
      return determineContext(frame).getObject(slot);
    }

    protected final boolean isUninitialized(final VirtualFrame frame) {
      return slot.getKind() == FrameSlotKind.Illegal;
    }
  }

  @NodeChild(value = "exp", type = ExpressionNode.class)
  public abstract static class NonLocalVariableWriteNode extends NonLocalVariableNode {

    public NonLocalVariableWriteNode(final int contextLevel, final FrameSlot slot) {
      super(contextLevel, slot);
    }

    @Specialization(guards = "isBoolKind(frame)")
    public final boolean writeBoolean(final VirtualFrame frame, final boolean expValue) {
      determineContext(frame).setBoolean(slot, expValue);
      return expValue;
    }

    @Specialization(guards = "isLongKind(frame)")
    public final long writeLong(final VirtualFrame frame, final long expValue) {
      determineContext(frame).setLong(slot, expValue);
      return expValue;
    }

    @Specialization(guards = "isDoubleKind(frame)")
    public final double writeDouble(final VirtualFrame frame, final double expValue) {
      determineContext(frame).setDouble(slot, expValue);
      return expValue;
    }

    @Specialization(replaces = {"writeBoolean", "writeLong", "writeDouble"})
    public final Object writeGeneric(final VirtualFrame frame, final Object expValue) {
      ensureObjectKind();
      determineContext(frame).setObject(slot, expValue);
      return expValue;
    }

    protected final boolean isBoolKind(final VirtualFrame frame) {
      if (slot.getKind() == FrameSlotKind.Boolean) {
        return true;
      }
      if (slot.getKind() == FrameSlotKind.Illegal) {
        transferToInterpreter("LocalVar.writeBoolToUninit");
        slot.setKind(FrameSlotKind.Boolean);
        return true;
      }
      return false;
    }

    protected final boolean isLongKind(final VirtualFrame frame) {
      if (slot.getKind() == FrameSlotKind.Long) {
        return true;
      }
      if (slot.getKind() == FrameSlotKind.Illegal) {
        transferToInterpreter("LocalVar.writeIntToUninit");
        slot.setKind(FrameSlotKind.Long);
        return true;
      }
      return false;
    }

    protected final boolean isDoubleKind(final VirtualFrame frame) {
      if (slot.getKind() == FrameSlotKind.Double) {
        return true;
      }
      if (slot.getKind() == FrameSlotKind.Illegal) {
        transferToInterpreter("LocalVar.writeDoubleToUninit");
        slot.setKind(FrameSlotKind.Double);
        return true;
      }
      return false;
    }

    protected final void ensureObjectKind() {
      if (slot.getKind() != FrameSlotKind.Object) {
        transferToInterpreter("LocalVar.writeObjectToUninit");
        slot.setKind(FrameSlotKind.Object);
      }
    }
  }
}
