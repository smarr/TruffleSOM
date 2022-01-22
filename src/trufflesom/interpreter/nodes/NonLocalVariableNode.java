package trufflesom.interpreter.nodes;

import static trufflesom.interpreter.TruffleCompiler.transferToInterpreter;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import bd.inlining.ScopeAdaptationVisitor;
import bd.inlining.ScopeAdaptationVisitor.ScopeElement;
import bd.tools.nodes.Invocation;
import trufflesom.compiler.Variable.Local;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


public abstract class NonLocalVariableNode extends ContextualNode
    implements Invocation<SSymbol> {

  protected final FrameSlot       slot;
  protected final Local           local;
  protected final FrameDescriptor descriptor;

  private NonLocalVariableNode(final int contextLevel, final Local local) {
    super(contextLevel);
    this.local = local;
    this.descriptor = local.getFrameDescriptor();
    this.slot = local.getSlot();
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
      return descriptor.getFrameSlotKind(slot) == FrameSlotKind.Illegal;
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
      if (descriptor.getFrameSlotKind(slot) == FrameSlotKind.Boolean) {
        return true;
      }
      if (descriptor.getFrameSlotKind(slot) == FrameSlotKind.Illegal) {
        transferToInterpreter("LocalVar.writeBoolToUninit");
        descriptor.setFrameSlotKind(slot, FrameSlotKind.Boolean);
        return true;
      }
      return false;
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

    protected final void ensureObjectKind() {
      if (descriptor.getFrameSlotKind(slot) != FrameSlotKind.Object) {
        transferToInterpreter("LocalVar.writeObjectToUninit");
        descriptor.setFrameSlotKind(slot, FrameSlotKind.Object);
      }
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      inliner.updateWrite(local, this, getExp(), contextLevel);
    }
  }

  public abstract static class NonLocalVariableIncNode extends NonLocalVariableNode {

    private final long incValue;

    public NonLocalVariableIncNode(final int contextLevel, final Local local,
        final long incValue) {
      super(contextLevel, local);
      this.incValue = incValue;
    }

    @Specialization(guards = "ctx.isLong(slot)", rewriteOn = {FrameSlotTypeException.class})
    public final long doLong(final VirtualFrame frame,
        @Bind("determineContext(frame)") final MaterializedFrame ctx)
        throws FrameSlotTypeException {
      long current = ctx.getLong(slot);
      long result = Math.addExact(current, incValue);
      ctx.setLong(slot, result);
      return result;
    }

    @Specialization(guards = "ctx.isDouble(slot)", rewriteOn = {FrameSlotTypeException.class})
    public final double doDouble(final VirtualFrame frame,
        @Bind("determineContext(frame)") final MaterializedFrame ctx)
        throws FrameSlotTypeException {
      double current = ctx.getDouble(slot);
      double result = current + incValue;
      ctx.setDouble(slot, result);
      return result;
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {

      ScopeElement<? extends Node> se = inliner.getAdaptedVar(local);
      if (se.var != local || se.contextLevel < contextLevel) {
        replace(se.var.getIncNode(se.contextLevel, incValue, sourceCoord));
      } else {
        assert contextLevel == se.contextLevel;
      }

    }
  }
}
