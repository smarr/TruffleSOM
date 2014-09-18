package som.interpreter.nodes;

import static som.interpreter.SNodeFactory.createLocalVariableWrite;
import static som.interpreter.TruffleCompiler.transferToInterpreter;
import som.compiler.Variable;
import som.compiler.Variable.Local;
import som.interpreter.Inliner;
import som.interpreter.nodes.LocalVariableNodeFactory.LocalSuperReadNodeFactory;
import som.vm.constants.Nil;
import som.vmobjects.SObject;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;


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

    public LocalVariableReadNode(final Variable variable,
        final SourceSection source) {
      this(variable.slot, source);
    }

    public LocalVariableReadNode(final LocalVariableReadNode node) {
      this(node.slot, node.getSourceSection());
    }

    public LocalVariableReadNode(final FrameSlot slot,
        final SourceSection source) {
      super(slot, source);
    }

    @Specialization(guards = "isUninitialized")
    public final SObject doNil() {
      return Nil.nilObject;
    }

    @Specialization(guards = "isInitialized")
    public final Object doObject(final VirtualFrame frame) {
      return frame.getValue(slot);
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

  public abstract static class LocalSuperReadNode
                       extends LocalVariableReadNode implements ISuperReadNode {
    private final SSymbol holderClass;
    private final boolean isClassSide;

    public LocalSuperReadNode(final Variable variable, final SSymbol holderClass,
        final boolean isClassSide, final SourceSection source) {
      this(variable.slot, holderClass, isClassSide, source);
    }

    public LocalSuperReadNode(final FrameSlot slot, final SSymbol holderClass,
        final boolean isClassSide, final SourceSection source) {
      super(slot, source);
      this.holderClass = holderClass;
      this.isClassSide = isClassSide;
    }

    public LocalSuperReadNode(final LocalSuperReadNode node) {
      this(node.slot, node.holderClass, node.isClassSide,
          node.getSourceSection());
    }

    @Override
    public final SSymbol getHolderClass() { return holderClass; }
    @Override
    public final boolean isClassSide()    { return isClassSide; }

    @Override
    public void replaceWithIndependentCopyForInlining(final Inliner inliner) {
      FrameSlot slot = inliner.getLocalFrameSlot(this.slot.getIdentifier());
      assert slot != null;
      replace(LocalSuperReadNodeFactory.create(slot, holderClass, isClassSide, getSourceSection()));
    }
  }

  @NodeChild(value = "exp", type = ExpressionNode.class)
  public abstract static class LocalVariableWriteNode extends LocalVariableNode {

    public LocalVariableWriteNode(final Local variable, final SourceSection source) {
      super(variable.slot, source);
    }

    public LocalVariableWriteNode(final LocalVariableWriteNode node) {
      super(node.slot, node.getSourceSection());
    }

    public LocalVariableWriteNode(final FrameSlot slot, final SourceSection source) {
      super(slot, source);
    }

    public abstract ExpressionNode getExp();

    @Specialization
    public final Object writeGeneric(final VirtualFrame frame, final Object expValue) {
      ensureObjectKind();
      frame.setObject(slot, expValue);
      return expValue;
    }

    protected final void ensureObjectKind() {
      if (slot.getKind() != FrameSlotKind.Object) {
        transferToInterpreter("LocalVar.writeObjectToUninit");
        slot.setKind(FrameSlotKind.Object);
      }
    }

    @Override
    public final void replaceWithIndependentCopyForInlining(final Inliner inliner) {
      CompilerAsserts.neverPartOfCompilation("replaceWithIndependentCopyForInlining");

      if (getParent() instanceof ArgumentInitializationNode) {
        FrameSlot varSlot = inliner.getLocalFrameSlot(getSlotIdentifier());
        assert varSlot != null;
        replace(createLocalVariableWrite(varSlot, getExp(), getSourceSection()));
      } else {
        throw new RuntimeException("Should not be part of an uninitalized tree. And this should only be done with uninitialized trees.");
      }
    }
  }
}
