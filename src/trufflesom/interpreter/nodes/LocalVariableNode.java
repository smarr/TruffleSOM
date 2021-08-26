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


public abstract class LocalVariableNode extends ExpressionNode implements Invocation<SSymbol> {
  protected final Local local;

  // TODO: We currently assume that there is a 1:1 mapping between lexical contexts
  // and frame descriptors, which is apparently not strictly true anymore in Truffle 1.0.0.
  // Generally, we also need to revise everything in this area and address issue SOMns#240.
  private LocalVariableNode(final Local local) {
    this.local = local;
  }

  public final Object getSlotIdentifier() {
    return local.getSlot().getIdentifier();
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

    @Specialization(guards = "local.isUninitialized(frame)")
    public final SObject doNil(final VirtualFrame frame) {
      return Nil.nilObject;
    }

    @Specialization(guards = {"frame.isBoolean(local.getSlot())"},
        rewriteOn = {FrameSlotTypeException.class})
    public final boolean doBoolean(final VirtualFrame frame) throws FrameSlotTypeException {
      return frame.getBoolean(local.getSlot());
    }

    @Specialization(guards = {"frame.isLong(local.getSlot())"},
        rewriteOn = {FrameSlotTypeException.class})
    public final long doLong(final VirtualFrame frame) throws FrameSlotTypeException {
      return frame.getLong(local.getSlot());
    }

    @Specialization(guards = {"frame.isDouble(local.getSlot())"},
        rewriteOn = {FrameSlotTypeException.class})
    public final double doDouble(final VirtualFrame frame) throws FrameSlotTypeException {
      return frame.getDouble(local.getSlot());
    }

    @Specialization(guards = {"frame.isObject(local.getSlot())"},
        replaces = {"doBoolean", "doLong", "doDouble"},
        rewriteOn = {FrameSlotTypeException.class})
    public final Object doObject(final VirtualFrame frame) throws FrameSlotTypeException {
      return frame.getObject(local.getSlot());
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

    @Specialization(guards = "local.isBoolKind(frame)")
    public final boolean writeBoolean(final VirtualFrame frame, final boolean expValue) {
      frame.setBoolean(local.getSlot(), expValue);
      return expValue;
    }

    @Specialization(guards = "local.isLongKind(frame)")
    public final long writeLong(final VirtualFrame frame, final long expValue) {
      frame.setLong(local.getSlot(), expValue);
      return expValue;
    }

    @Specialization(guards = "local.isDoubleKind(frame)")
    public final double writeDouble(final VirtualFrame frame, final double expValue) {
      frame.setDouble(local.getSlot(), expValue);
      return expValue;
    }

    @Specialization(replaces = {"writeBoolean", "writeLong", "writeDouble"})
    public final Object writeGeneric(final VirtualFrame frame, final Object expValue) {
      local.makeObject();
      frame.setObject(local.getSlot(), expValue);
      return expValue;
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      inliner.updateWrite(local, this, getExp(), 0);
    }
  }
}
