package trufflesom.interpreter.nodes;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;

import bdt.inlining.ScopeAdaptationVisitor;
import bdt.tools.nodes.Invocation;
import trufflesom.compiler.Variable.Local;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


public abstract class LocalVariableNode extends NoPreEvalExprNode implements Invocation<SSymbol> {
  protected final int   slotIndex;
  protected final Local local;

  // TODO: We currently assume that there is a 1:1 mapping between lexical contexts
  // and frame descriptors, which is apparently not strictly true anymore in Truffle 1.0.0.
  // Generally, we also need to revise everything in this area and address issue SOMns#240.
  private LocalVariableNode(final Local local) {
    this.local = local;
    this.slotIndex = local.getIndex();
  }

  public Local getLocal() {
    return local;
  }

  @Override
  public final SSymbol getInvocationIdentifier() {
    return local.name;
  }

  public abstract static class LocalVariableReadNode extends LocalVariableNode {

    public LocalVariableReadNode(final Local variable) {
      super(variable);
    }

    public LocalVariableReadNode(final LocalVariableReadNode node) {
      this(node.local);
    }

    @Specialization(guards = "isUninitialized(frame)")
    public final SObject doNil(final VirtualFrame frame) {
      return Nil.nilObject;
    }

    @Specialization(guards = {"frame.isBoolean(slotIndex)"},
        rewriteOn = {FrameSlotTypeException.class})
    public final boolean doBoolean(final VirtualFrame frame) throws FrameSlotTypeException {
      return frame.getBoolean(slotIndex);
    }

    @Specialization(guards = {"frame.isLong(slotIndex)"},
        rewriteOn = {FrameSlotTypeException.class})
    public final long doLong(final VirtualFrame frame) throws FrameSlotTypeException {
      return frame.getLong(slotIndex);
    }

    @Specialization(guards = {"frame.isDouble(slotIndex)"},
        rewriteOn = {FrameSlotTypeException.class})
    public final double doDouble(final VirtualFrame frame) throws FrameSlotTypeException {
      return frame.getDouble(slotIndex);
    }

    @Specialization(guards = {"frame.isObject(slotIndex)"},
        replaces = {"doBoolean", "doLong", "doDouble"},
        rewriteOn = {FrameSlotTypeException.class})
    public final Object doObject(final VirtualFrame frame) throws FrameSlotTypeException {
      return frame.getObject(slotIndex);
    }

    protected final boolean isUninitialized(final VirtualFrame frame) {
      return local.getFrameDescriptor().getSlotKind(slotIndex) == FrameSlotKind.Illegal;
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      inliner.updateRead(local, this, 0);
    }
  }

  @NodeChild(value = "exp", type = ExpressionNode.class)
  public abstract static class LocalVariableWriteNode extends LocalVariableNode {

    public LocalVariableWriteNode(final Local variable) {
      super(variable);
    }

    public LocalVariableWriteNode(final LocalVariableWriteNode node) {
      super(node.local);
    }

    public abstract ExpressionNode getExp();

    @Specialization(guards = "isBoolKind(expValue)")
    public final boolean writeBoolean(final VirtualFrame frame, final boolean expValue) {
      frame.setBoolean(slotIndex, expValue);
      return expValue;
    }

    @Specialization(guards = "isLongKind(expValue)")
    public final long writeLong(final VirtualFrame frame, final long expValue) {
      frame.setLong(slotIndex, expValue);
      return expValue;
    }

    @Specialization(guards = "isDoubleKind(expValue)")
    public final double writeDouble(final VirtualFrame frame, final double expValue) {
      frame.setDouble(slotIndex, expValue);
      return expValue;
    }

    @Specialization(replaces = {"writeBoolean", "writeLong", "writeDouble"})
    public final Object writeGeneric(final VirtualFrame frame, final Object expValue) {
      local.getFrameDescriptor().setSlotKind(slotIndex, FrameSlotKind.Object);
      frame.setObject(slotIndex, expValue);
      return expValue;
    }

    // uses expValue to make sure guard is not converted to assertion
    protected final boolean isBoolKind(final boolean expValue) {
      if (local.getFrameDescriptor().getSlotKind(slotIndex) == FrameSlotKind.Boolean) {
        return true;
      }
      if (local.getFrameDescriptor().getSlotKind(slotIndex) == FrameSlotKind.Illegal) {
        local.getFrameDescriptor().setSlotKind(slotIndex, FrameSlotKind.Boolean);
        return true;
      }
      return false;
    }

    // uses expValue to make sure guard is not converted to assertion
    protected final boolean isLongKind(final long expValue) {
      if (local.getFrameDescriptor().getSlotKind(slotIndex) == FrameSlotKind.Long) {
        return true;
      }
      if (local.getFrameDescriptor().getSlotKind(slotIndex) == FrameSlotKind.Illegal) {
        local.getFrameDescriptor().setSlotKind(slotIndex, FrameSlotKind.Long);
        return true;
      }
      return false;
    }

    // uses expValue to make sure guard is not converted to assertion
    protected final boolean isDoubleKind(final double expValue) {
      if (local.getFrameDescriptor().getSlotKind(slotIndex) == FrameSlotKind.Double) {
        return true;
      }
      if (local.getFrameDescriptor().getSlotKind(slotIndex) == FrameSlotKind.Illegal) {
        local.getFrameDescriptor().setSlotKind(slotIndex, FrameSlotKind.Double);
        return true;
      }
      return false;
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      inliner.updateWrite(local, this, getExp(), 0);
    }
  }
}
