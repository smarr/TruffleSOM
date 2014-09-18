package som.interpreter.nodes;

import static som.interpreter.TruffleCompiler.transferToInterpreter;
import som.interpreter.Inliner;
import som.interpreter.nodes.NonLocalVariableNodeFactory.NonLocalSuperReadNodeFactory;
import som.vm.constants.Nil;
import som.vmobjects.SObject;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;


public abstract class NonLocalVariableNode extends ContextualNode {

  protected final FrameSlot slot;

  private NonLocalVariableNode(final int contextLevel, final FrameSlot slot,
      final FrameSlot localSelf, final SourceSection source) {
    super(contextLevel, localSelf, source);
    this.slot = slot;
  }

  public abstract static class NonLocalVariableReadNode extends NonLocalVariableNode {

    public NonLocalVariableReadNode(final int contextLevel,
        final FrameSlot slot, final FrameSlot localSelf, final SourceSection source) {
      super(contextLevel, slot, localSelf, source);
    }

    public NonLocalVariableReadNode(final NonLocalVariableReadNode node) {
      this(node.contextLevel, node.slot, node.localSelf, node.getSourceSection());
    }

    @Specialization(guards = "isUninitialized")
    public final SObject doNil() {
      return Nil.nilObject;
    }


    @Specialization(guards = "isInitialized")
    public final Object doObject(final VirtualFrame frame) {
      return determineContext(frame).getValue(slot);
    }

    protected final boolean isInitialized() {
      return slot.getKind() != FrameSlotKind.Illegal;
    }

    protected final boolean isUninitialized() {
      return slot.getKind() == FrameSlotKind.Illegal;
    }

    @Override
    public void replaceWithIndependentCopyForInlining(final Inliner inliner) {
      throw new RuntimeException("Should not be part of an uninitalized tree. And this should only be done with uninitialized trees.");
    }
  }

  public abstract static class NonLocalSuperReadNode
                       extends NonLocalVariableReadNode implements ISuperReadNode {
    private final SSymbol holderClass;
    private final boolean isClassSide;

    public NonLocalSuperReadNode(final int contextLevel, final FrameSlot slot,
        final FrameSlot localSelf, final SSymbol holderClass,
        final boolean isClassSide, final SourceSection source) {
      super(contextLevel, slot, localSelf, source);
      this.holderClass = holderClass;
      this.isClassSide = isClassSide;
    }

    public NonLocalSuperReadNode(final NonLocalSuperReadNode node) {
      this(node.contextLevel, node.slot, node.localSelf, node.holderClass,
          node.isClassSide, node.getSourceSection());
    }

    @Override
    public final SSymbol getHolderClass() { return holderClass; }
    @Override
    public final boolean isClassSide()    { return isClassSide; }

    @Override
    public void replaceWithIndependentCopyForInlining(final Inliner inliner) {
      FrameSlot varSlot = inliner.getFrameSlot(this, slot.getIdentifier());
      assert varSlot != null;
      FrameSlot selfSlot = inliner.getFrameSlot(this, slot.getIdentifier());
      assert selfSlot != null;
      replace(NonLocalSuperReadNodeFactory.create(this.contextLevel, varSlot, selfSlot, holderClass, isClassSide, getSourceSection()));
    }
  }

  @NodeChild(value = "exp", type = ExpressionNode.class)
  public abstract static class NonLocalVariableWriteNode extends NonLocalVariableNode {

    public NonLocalVariableWriteNode(final int contextLevel,
        final FrameSlot slot, final FrameSlot localSelf, final SourceSection source) {
      super(contextLevel, slot, localSelf, source);
    }

    public NonLocalVariableWriteNode(final NonLocalVariableWriteNode node) {
      this(node.contextLevel, node.slot, node.localSelf, node.getSourceSection());
    }

    @Specialization
    public final Object write(final VirtualFrame frame, final Object expValue) {
      ensureObjectKind();
      determineContext(frame).setObject(slot, expValue);
      return expValue;
    }

    protected final void ensureObjectKind() {
      if (slot.getKind() != FrameSlotKind.Object) {
        transferToInterpreter("LocalVar.writeObjectToUninit");
        slot.setKind(FrameSlotKind.Object);
      }
    }
  }
}
