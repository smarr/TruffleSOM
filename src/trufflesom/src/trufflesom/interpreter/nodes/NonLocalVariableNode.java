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

import trufflesom.bdt.inlining.ScopeAdaptationVisitor;
import trufflesom.bdt.tools.nodes.Invocation;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.Method.OpBuilder;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


public abstract class NonLocalVariableNode extends ContextualNode
    implements Invocation<String> {

  protected final int   slotIndex;
  protected final Local local;

  protected NonLocalVariableNode(final int contextLevel, final Local local) {
    super(contextLevel);
    this.local = local;
    this.slotIndex = local.getIndex();
  }

  public boolean isSameLocal(final NonLocalVariableNode node) {
    return local.equals(node.local);
  }

  @Override
  public String getInvocationIdentifier() {
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
    public static final SObject doNil(@SuppressWarnings("unused") final VirtualFrame frame) {
      return Nil.nilObject;
    }

    @Specialization(guards = {"ctx.isBoolean(slotIndex)"},
        rewriteOn = {FrameSlotTypeException.class})
    public final boolean doBoolean(@SuppressWarnings("unused") final VirtualFrame frame,
        @Shared("all") @Bind("determineContext(frame)") final MaterializedFrame ctx)
        throws FrameSlotTypeException {
      return ctx.getBoolean(slotIndex);
    }

    @Specialization(guards = {"ctx.isLong(slotIndex)"},
        rewriteOn = {FrameSlotTypeException.class})
    public final long doLong(@SuppressWarnings("unused") final VirtualFrame frame,
        @Shared("all") @Bind("determineContext(frame)") final MaterializedFrame ctx)
        throws FrameSlotTypeException {
      return ctx.getLong(slotIndex);
    }

    @Specialization(guards = {"ctx.isDouble(slotIndex)"},
        rewriteOn = {FrameSlotTypeException.class})
    public final double doDouble(@SuppressWarnings("unused") final VirtualFrame frame,
        @Shared("all") @Bind("determineContext(frame)") final MaterializedFrame ctx)
        throws FrameSlotTypeException {
      return ctx.getDouble(slotIndex);
    }

    @Specialization(guards = {"ctx.isObject(slotIndex)"},
        replaces = {"doBoolean", "doLong", "doDouble"},
        rewriteOn = {FrameSlotTypeException.class})
    public final Object doObject(@SuppressWarnings("unused") final VirtualFrame frame,
        @Shared("all") @Bind("determineContext(frame)") final MaterializedFrame ctx)
        throws FrameSlotTypeException {
      return ctx.getObject(slotIndex);
    }

    protected final boolean isUninitialized(
        @SuppressWarnings("unused") final VirtualFrame frame) {
      return local.getFrameDescriptor().getSlotKind(slotIndex) == FrameSlotKind.Illegal;
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      inliner.updateRead(local, this, contextLevel);
    }

    @Override
    public void constructOperation(final OpBuilder opBuilder) {
      opBuilder.dsl.beginLoadLocalMaterialized(opBuilder.getLocal(local));
      opBuilder.dsl.emitDetermineContextOp(contextLevel);
      opBuilder.dsl.endLoadLocalMaterialized();
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

    protected final boolean isBoolKind(@SuppressWarnings("unused") final VirtualFrame frame) {
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

    protected final boolean isLongKind(@SuppressWarnings("unused") final VirtualFrame frame) {
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

    protected final boolean isDoubleKind(
        @SuppressWarnings("unused") final VirtualFrame frame) {
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

    @Override
    public void constructOperation(final OpBuilder opBuilder) {
      opBuilder.dsl.beginBlock();

      opBuilder.dsl.beginStoreLocalMaterialized(opBuilder.getLocal(local));
      opBuilder.dsl.emitDetermineContextOp(contextLevel);
      getExp().accept(opBuilder);
      opBuilder.dsl.endStoreLocalMaterialized();

      // TOOD: can we get something more optimal for this??
      opBuilder.dsl.beginLoadLocalMaterialized(opBuilder.getLocal(local));
      opBuilder.dsl.emitDetermineContextOp(contextLevel);
      opBuilder.dsl.endLoadLocalMaterialized();

      opBuilder.dsl.endBlock();
    }
  }
}
