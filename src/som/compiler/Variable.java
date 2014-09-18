package som.compiler;

import static som.interpreter.SNodeFactory.createSuperRead;
import static som.interpreter.SNodeFactory.createVariableRead;
import static som.interpreter.SNodeFactory.createVariableWrite;
import static som.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;
import som.interpreter.nodes.ContextualNode;
import som.interpreter.nodes.ExpressionNode;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.source.SourceSection;

public abstract class Variable {
  public final String name;
  public final FrameSlot slot;

  Variable(final String name, final FrameSlot slot) {
    this.name      = name;
    this.slot      = slot;
  }

  @Override
  public String toString() {
    return getClass().getName() + "(" + name + ")";
  }

  public final FrameSlot getSlot() {
    return slot;
  }

  public final Object getSlotIdentifier() {
    return slot.getIdentifier();
  }

  public abstract Variable cloneForInlining(final FrameSlot inlinedSlot);

  public final ContextualNode getReadNode(final int contextLevel,
      final FrameSlot localSelf, final SourceSection source) {
    transferToInterpreterAndInvalidate("Variable.getReadNode");
    return createVariableRead(this, contextLevel, localSelf, source);
  }

  public final ExpressionNode getSuperReadNode(final int contextLevel,
      final SSymbol holderClass, final boolean classSide,
      final FrameSlot localSelf, final SourceSection source) {
    return createSuperRead(this, contextLevel, localSelf, holderClass, classSide, source);
  }

  public static final class Argument extends Variable {
    public final int index;

    Argument(final String name, final FrameSlot slot, final int index) {
      super(name, slot);
      this.index = index;
    }

    @Override
    public Variable cloneForInlining(final FrameSlot inlinedSlot) {
      Argument arg = new Argument(name, inlinedSlot, index);
      return arg;
    }

    public boolean isSelf() {
      return "self".equals(name) || "$blockSelf".equals(name);
    }
  }

  public static final class Local extends Variable {
    Local(final String name, final FrameSlot slot) {
      super(name, slot);
    }

    @Override
    public Variable cloneForInlining(final FrameSlot inlinedSlot) {
      Local local = new Local(name, inlinedSlot);
      return local;
    }

    public ExpressionNode getWriteNode(final int contextLevel,
        final FrameSlot localSelf,
        final ExpressionNode valueExpr, final SourceSection source) {
      transferToInterpreterAndInvalidate("Variable.getWriteNode");
      return createVariableWrite(this, contextLevel, localSelf, valueExpr, source);
    }
  }
}
