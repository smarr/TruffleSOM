package trufflesom.interpreter.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.SplitterForLexicallyEmbeddedCode;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SObject;


public abstract class LocalVariableNode extends ExpressionNode {
  protected final FrameSlot slot;

  private LocalVariableNode(final FrameSlot slot, final SourceSection source) {
    super(source);
    this.slot = slot;
  }

  public final Object getSlotIdentifier() {
    return slot.getIdentifier();
  }

  public abstract static class LocalVariableReadNode extends LocalVariableNode {

    public LocalVariableReadNode(final Local variable,
        final SourceSection source) {
      this(variable.getSlot(), source);
    }

    public LocalVariableReadNode(final LocalVariableReadNode node) {
      this(node.slot, node.getSourceSection());
    }

    public LocalVariableReadNode(final FrameSlot slot,
        final SourceSection source) {
      super(slot, source);
    }

    @Specialization(guards = "isUninitialized(frame)")
    public final SObject doNil(final VirtualFrame frame) {
      return Nil.nilObject;
    }

    protected boolean isBoolean(final VirtualFrame frame) {
      return frame.isBoolean(slot);
    }

    protected boolean isLong(final VirtualFrame frame) {
      return frame.isLong(slot);
    }

    protected boolean isDouble(final VirtualFrame frame) {
      return frame.isDouble(slot);
    }

    protected boolean isObject(final VirtualFrame frame) {
      return frame.isObject(slot);
    }

    @Specialization(guards = {"isBoolean(frame)"}, rewriteOn = {FrameSlotTypeException.class})
    public final boolean doBoolean(final VirtualFrame frame) throws FrameSlotTypeException {
      return frame.getBoolean(slot);
    }

    @Specialization(guards = {"isLong(frame)"}, rewriteOn = {FrameSlotTypeException.class})
    public final long doLong(final VirtualFrame frame) throws FrameSlotTypeException {
      return frame.getLong(slot);
    }

    @Specialization(guards = {"isDouble(frame)"}, rewriteOn = {FrameSlotTypeException.class})
    public final double doDouble(final VirtualFrame frame) throws FrameSlotTypeException {
      return frame.getDouble(slot);
    }

    @Specialization(guards = {"isObject(frame)"},
        replaces = {"doBoolean", "doLong", "doDouble"},
        rewriteOn = {FrameSlotTypeException.class})
    public final Object doObject(final VirtualFrame frame) throws FrameSlotTypeException {
      return frame.getObject(slot);
    }

    protected final boolean isUninitialized(final VirtualFrame frame) {
      return slot.getKind() == FrameSlotKind.Illegal;
    }
  }

  @NodeChild(value = "exp", type = ExpressionNode.class)
  public abstract static class LocalVariableWriteNode extends LocalVariableNode {

    public LocalVariableWriteNode(final Local variable, final SourceSection source) {
      super(variable.getSlot(), source);
    }

    public LocalVariableWriteNode(final LocalVariableWriteNode node) {
      super(node.slot, node.getSourceSection());
    }

    public LocalVariableWriteNode(final FrameSlot slot, final SourceSection source) {
      super(slot, source);
    }

    public abstract ExpressionNode getExp();

    @Specialization(guards = "isBoolKind(expValue)")
    public final boolean writeBoolean(final VirtualFrame frame, final boolean expValue) {
      frame.setBoolean(slot, expValue);
      return expValue;
    }

    @Specialization(guards = "isLongKind(expValue)")
    public final long writeLong(final VirtualFrame frame, final long expValue) {
      frame.setLong(slot, expValue);
      return expValue;
    }

    @Specialization(guards = "isDoubleKind(expValue)")
    public final double writeDouble(final VirtualFrame frame, final double expValue) {
      frame.setDouble(slot, expValue);
      return expValue;
    }

    @Specialization(replaces = {"writeBoolean", "writeLong", "writeDouble"})
    public final Object writeGeneric(final VirtualFrame frame, final Object expValue) {
      slot.setKind(FrameSlotKind.Object);
      frame.setObject(slot, expValue);
      return expValue;
    }

    // uses expValue to make sure guard is not converted to assertion
    protected final boolean isBoolKind(final boolean expValue) {
      if (slot.getKind() == FrameSlotKind.Boolean) {
        return true;
      }
      if (slot.getKind() == FrameSlotKind.Illegal) {
        slot.setKind(FrameSlotKind.Boolean);
        return true;
      }
      return false;
    }

    // uses expValue to make sure guard is not converted to assertion
    protected final boolean isLongKind(final long expValue) {
      if (slot.getKind() == FrameSlotKind.Long) {
        return true;
      }
      if (slot.getKind() == FrameSlotKind.Illegal) {
        slot.setKind(FrameSlotKind.Long);
        return true;
      }
      return false;
    }

    // uses expValue to make sure guard is not converted to assertion
    protected final boolean isDoubleKind(final double expValue) {
      if (slot.getKind() == FrameSlotKind.Double) {
        return true;
      }
      if (slot.getKind() == FrameSlotKind.Illegal) {
        slot.setKind(FrameSlotKind.Double);
        return true;
      }
      return false;
    }

    @Override
    public final void replaceWithIndependentCopyForInlining(
        final SplitterForLexicallyEmbeddedCode inliner) {
      CompilerAsserts.neverPartOfCompilation("replaceWithIndependentCopyForInlining");
      throw new RuntimeException(
          "Should not be part of an uninitalized tree. And this should only be done with uninitialized trees.");
    }
  }
}
