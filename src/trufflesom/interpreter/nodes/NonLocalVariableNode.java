package trufflesom.interpreter.nodes;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

import bdt.inlining.ScopeAdaptationVisitor;
import bdt.tools.nodes.Invocation;
import trufflesom.compiler.Variable.Local;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


public abstract class NonLocalVariableNode extends ContextualNode
    implements Invocation<SSymbol> {

  protected final int   slotIndex;
  protected final Local local;

  private NonLocalVariableNode(final int contextLevel, final Local local) {
    super(contextLevel);
    this.local = local;
    this.slotIndex = local.getIndex();
  }

  @Override
  public SSymbol getInvocationIdentifier() {
    return local.name;
  }

  public Local getLocal() {
    return local;
  }

  public abstract static class NonLocalVariableReadNode extends NonLocalVariableNode {

    public NonLocalVariableReadNode(final int contextLevel, final Local local) {
      super(contextLevel, local);
    }

    @Specialization(guards = "isUninitialized(frame)")
    public final SObject doNil(final VirtualFrame frame) {
      return Nil.nilObject;
    }

    @Specialization(guards = {"ctx.isBoolean(slotIndex)"},
        rewriteOn = {FrameSlotTypeException.class})
    public final boolean doBoolean(final VirtualFrame frame,
        @Shared("all") @Bind("determineContext(frame)") final MaterializedFrame ctx)
        throws FrameSlotTypeException {
      return ctx.getBoolean(slotIndex);
    }

    @Specialization(guards = {"ctx.isLong(slotIndex)"},
        rewriteOn = {FrameSlotTypeException.class})
    public final long doLong(final VirtualFrame frame,
        @Shared("all") @Bind("determineContext(frame)") final MaterializedFrame ctx)
        throws FrameSlotTypeException {
      return ctx.getLong(slotIndex);
    }

    @Specialization(guards = {"ctx.isDouble(slotIndex)"},
        rewriteOn = {FrameSlotTypeException.class})
    public final double doDouble(final VirtualFrame frame,
        @Shared("all") @Bind("determineContext(frame)") final MaterializedFrame ctx)
        throws FrameSlotTypeException {
      return ctx.getDouble(slotIndex);
    }

    @Specialization(guards = {"ctx.isObject(slotIndex)"},
        replaces = {"doBoolean", "doLong", "doDouble"},
        rewriteOn = {FrameSlotTypeException.class})
    public final Object doObject(final VirtualFrame frame,
        @Shared("all") @Bind("determineContext(frame)") final MaterializedFrame ctx)
        throws FrameSlotTypeException {
      return ctx.getObject(slotIndex);
    }

    protected final boolean isUninitialized(final VirtualFrame frame) {
      return local.getFrameDescriptor().getSlotKind(slotIndex) == FrameSlotKind.Illegal;
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      inliner.updateRead(local, this, contextLevel);
    }
  }

  @NodeChild(value = "exp", type = ExpressionNode.class)
  public abstract static class NonLocalVariableWriteNode extends NonLocalVariableNode {

    public NonLocalVariableWriteNode(final int contextLevel, final Local local) {
      super(contextLevel, local);
    }

    public abstract ExpressionNode getExp();

    @Specialization(guards = "isBoolKind(frame)")
    public final boolean writeBoolean(final VirtualFrame frame, final boolean expValue) {
      determineContext(frame).setBoolean(slotIndex, expValue);
      return expValue;
    }

    @Specialization(guards = "isLongKind(frame)")
    public final long writeLong(final VirtualFrame frame, final long expValue) {
      determineContext(frame).setLong(slotIndex, expValue);
      return expValue;
    }

    @Specialization(guards = "isDoubleKind(frame)")
    public final double writeDouble(final VirtualFrame frame, final double expValue) {
      determineContext(frame).setDouble(slotIndex, expValue);
      return expValue;
    }

    @Specialization(replaces = {"writeBoolean", "writeLong", "writeDouble"})
    public final Object writeGeneric(final VirtualFrame frame, final Object expValue) {
      local.getFrameDescriptor().setSlotKind(slotIndex, FrameSlotKind.Object);
      determineContext(frame).setObject(slotIndex, expValue);
      return expValue;
    }

    protected final boolean isBoolKind(final VirtualFrame frame) {
      FrameDescriptor descriptor = local.getFrameDescriptor();
      FrameSlotKind kind = descriptor.getSlotKind(slotIndex);
      if (kind == FrameSlotKind.Boolean) {
        return true;
      }
      if (kind == FrameSlotKind.Illegal) {
        descriptor.setSlotKind(slotIndex, FrameSlotKind.Boolean);
        return true;
      }
      return false;
    }

    protected final boolean isLongKind(final VirtualFrame frame) {
      FrameDescriptor descriptor = local.getFrameDescriptor();
      FrameSlotKind kind = descriptor.getSlotKind(slotIndex);
      if (kind == FrameSlotKind.Long) {
        return true;
      }
      if (kind == FrameSlotKind.Illegal) {
        descriptor.setSlotKind(slotIndex, FrameSlotKind.Long);
        return true;
      }
      return false;
    }

    protected final boolean isDoubleKind(final VirtualFrame frame) {
      FrameDescriptor descriptor = local.getFrameDescriptor();
      FrameSlotKind kind = descriptor.getSlotKind(slotIndex);
      if (kind == FrameSlotKind.Double) {
        return true;
      }
      if (kind == FrameSlotKind.Illegal) {
        descriptor.setSlotKind(slotIndex, FrameSlotKind.Double);
        return true;
      }
      return false;
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      inliner.updateWrite(local, this, getExp(), contextLevel);
    }
  }
}
