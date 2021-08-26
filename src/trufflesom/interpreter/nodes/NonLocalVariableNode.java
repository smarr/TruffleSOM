package trufflesom.interpreter.nodes;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;

import bd.inlining.ScopeAdaptationVisitor;
import bd.tools.nodes.Invocation;
import trufflesom.compiler.Variable.Local;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


public abstract class NonLocalVariableNode extends ContextualNode
    implements Invocation<SSymbol> {

  protected final Local local;

  private NonLocalVariableNode(final int contextLevel, final Local local) {
    super(contextLevel);
    this.local = local;
  }

  @Override
  public SSymbol getInvocationIdentifier() {
    return local.name;
  }

  public abstract static class NonLocalVariableReadNode extends NonLocalVariableNode {

    public NonLocalVariableReadNode(final int contextLevel, final Local local) {
      super(contextLevel, local);
    }

    @Specialization(guards = "local.isUninitialized(frame)")
    public final SObject doNil(final VirtualFrame frame) {
      return Nil.nilObject;
    }

    @Specialization(guards = {"determineContext(frame).isBoolean(local.getSlot())"},
        rewriteOn = {FrameSlotTypeException.class})
    public final boolean doBoolean(final VirtualFrame frame) throws FrameSlotTypeException {
      return determineContext(frame).getBoolean(local.getSlot());
    }

    @Specialization(guards = {"determineContext(frame).isLong(local.getSlot())"},
        rewriteOn = {FrameSlotTypeException.class})
    public final long doLong(final VirtualFrame frame) throws FrameSlotTypeException {
      return determineContext(frame).getLong(local.getSlot());
    }

    @Specialization(guards = {"determineContext(frame).isDouble(local.getSlot())"},
        rewriteOn = {FrameSlotTypeException.class})
    public final double doDouble(final VirtualFrame frame) throws FrameSlotTypeException {
      return determineContext(frame).getDouble(local.getSlot());
    }

    @Specialization(guards = {"determineContext(frame).isObject(local.getSlot())"},
        replaces = {"doBoolean", "doLong", "doDouble"},
        rewriteOn = {FrameSlotTypeException.class})
    public final Object doObject(final VirtualFrame frame) throws FrameSlotTypeException {
      return determineContext(frame).getObject(local.getSlot());
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

    @Specialization(guards = "local.isBoolKind(frame)")
    public final boolean writeBoolean(final VirtualFrame frame, final boolean expValue) {
      determineContext(frame).setBoolean(local.getSlot(), expValue);
      return expValue;
    }

    @Specialization(guards = "local.isLongKind(frame)")
    public final long writeLong(final VirtualFrame frame, final long expValue) {
      determineContext(frame).setLong(local.getSlot(), expValue);
      return expValue;
    }

    @Specialization(guards = "local.isDoubleKind(frame)")
    public final double writeDouble(final VirtualFrame frame, final double expValue) {
      determineContext(frame).setDouble(local.getSlot(), expValue);
      return expValue;
    }

    @Specialization(replaces = {"writeBoolean", "writeLong", "writeDouble"})
    public final Object writeGeneric(final VirtualFrame frame, final Object expValue) {
      local.makeObject();
      determineContext(frame).setObject(local.getSlot(), expValue);
      return expValue;
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      inliner.updateWrite(local, this, getExp(), contextLevel);
    }
  }
}
