package trufflesom.interpreter.nodes.bc;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import trufflesom.compiler.Variable.Local;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SObject;


public abstract class LocalNode {
  public abstract static class LocalPush extends Node {
    public abstract Object execute(VirtualFrame frame, Local local);

    @Specialization(guards = "local.isUninitialized(frame)")
    public final SObject doNil(final VirtualFrame frame, final Local local) {
      return Nil.nilObject;
    }

    @Specialization(guards = {"frame.isBoolean(local.getSlot())"},
        rewriteOn = {FrameSlotTypeException.class})
    public final boolean doBoolean(final VirtualFrame frame, final Local local)
        throws FrameSlotTypeException {
      return frame.getBoolean(local.getSlot());
    }

    @Specialization(guards = {"frame.isLong(local.getSlot())"},
        rewriteOn = {FrameSlotTypeException.class})
    public final long doLong(final VirtualFrame frame, final Local local)
        throws FrameSlotTypeException {
      return frame.getLong(local.getSlot());
    }

    @Specialization(guards = {"frame.isDouble(local.getSlot())"},
        rewriteOn = {FrameSlotTypeException.class})
    public final double doDouble(final VirtualFrame frame, final Local local)
        throws FrameSlotTypeException {
      return frame.getDouble(local.getSlot());
    }

    @Specialization(guards = {"frame.isObject(local.getSlot())"},
        replaces = {"doBoolean", "doLong", "doDouble"},
        rewriteOn = {FrameSlotTypeException.class})
    public final Object doObject(final VirtualFrame frame, final Local local)
        throws FrameSlotTypeException {
      return frame.getObject(local.getSlot());
    }
  }

  public abstract static class LocalPop extends Node {

    public abstract void execute(VirtualFrame frame, Local local, Object value);

    @Specialization(guards = "local.isBoolKind(frame)")
    public final void writeBoolean(final VirtualFrame frame, final Local local,
        final Boolean expValue) {
      frame.setBoolean(local.getSlot(), expValue);
    }

    @Specialization(guards = "local.isLongKind(frame)")
    public final void writeLong(final VirtualFrame frame, final Local local,
        final Long expValue) {
      frame.setLong(local.getSlot(), expValue);
    }

    @Specialization(guards = "local.isDoubleKind(frame)")
    public final void writeDouble(final VirtualFrame frame, final Local local,
        final Double expValue) {
      frame.setDouble(local.getSlot(), expValue);
    }

    @Specialization(replaces = {"writeBoolean", "writeLong", "writeDouble"})
    public final void writeGeneric(final VirtualFrame frame, final Local local,
        final Object expValue) {
      local.makeObject();
      frame.setObject(local.getSlot(), expValue);
    }

  }
}
