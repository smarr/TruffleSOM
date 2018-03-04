package som.compiler;

import static som.interpreter.SNodeFactory.createArgumentRead;
import static som.interpreter.SNodeFactory.createLocalVarRead;
import static som.interpreter.SNodeFactory.createSuperRead;
import static som.interpreter.SNodeFactory.createVariableWrite;
import static som.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.source.SourceSection;

import som.interpreter.nodes.ExpressionNode;
import som.vmobjects.SSymbol;


public abstract class Variable
    implements bd.inlining.Variable<ExpressionNode, AccessNodeState> {
  public final SSymbol       name;
  public final SourceSection source;

  Variable(final SSymbol name, final SourceSection source) {
    this.name = name;
    this.source = source;
  }

  @Override
  public String toString() {
    return getClass().getName() + "(" + name + ")";
  }

  @Override
  public abstract ExpressionNode getReadNode(int contextLevel, SourceSection source);



  public final ExpressionNode getSuperReadNode(final int contextLevel,
      final SSymbol holderClass, final boolean classSide,
      final SourceSection source) {
    isRead = true;
    if (contextLevel > 0) {
      isReadOutOfContext = true;
  @Override
  public boolean equals(final Object o) {
    assert o != null;
    if (o == this) {
      return true;
    }
    if (!(o instanceof Variable)) {
      return false;
    }
    Variable var = (Variable) o;
    if (var.source == source) {
      assert name == var.name : "Defined in the same place, but names not equal?";
      return true;
    }
    assert source == null || !source.equals(
        var.source) : "Why are there multiple objects for this source section? might need to fix comparison above";
    return false;
  }
    }
    return createSuperRead(contextLevel, holderClass, classSide, source);
  }

  public static final class Argument extends Variable {
    public final int index;

    Argument(final SSymbol name, final int index, final SourceSection source) {
      super(name, source);
      this.index = index;
    }

    public boolean isSelf(final Universe universe) {
      return universe.symSelf == name || universe.symBlockSelf == name;
    }

    }

    @Override
    public ExpressionNode getReadNode(final int contextLevel,
        final SourceSection source) {
      transferToInterpreterAndInvalidate("Variable.getReadNode");
      return createArgumentRead(this, contextLevel, source);
    }
  }

  public static final class Local extends Variable {
    @CompilationFinal private FrameSlot slot;

    Local(final SSymbol name, final SourceSection source) {
      super(name, source);
    }

    public void init(final FrameSlot slot) {
      this.slot = slot;
    }

    @Override
    public ExpressionNode getReadNode(final int contextLevel, final SourceSection source) {
      transferToInterpreterAndInvalidate("Variable.getReadNode");
      return createLocalVarRead(this, contextLevel, source);
    }

    public FrameSlot getSlot() {
      return slot;
    }

    public Object getSlotIdentifier() {
      return slot.getIdentifier();
    }

    public Local cloneForInlining(final FrameSlot inlinedSlot) {
      Local local = new Local(name, inlinedSlot);
      return local;
    }

    @Override
    public ExpressionNode getWriteNode(final int contextLevel,
        final ExpressionNode valueExpr, final SourceSection source) {
      transferToInterpreterAndInvalidate("Variable.getWriteNode");
      return createVariableWrite(this, contextLevel, valueExpr, source);
    }
  }

  public static final class Internal extends Variable {
    @CompilationFinal private FrameSlot slot;

    public Internal(final SSymbol name) {
      super(name, null);
    }

    public void init(final FrameSlot slot) {
      assert this.slot == null && slot != null;
      this.slot = slot;
    }

    public FrameSlot getSlot() {
      assert slot != null : "Should have been initialized with init(.)";
      return slot;
    }

    @Override
    public ExpressionNode getReadNode(final int contextLevel, final SourceSection source) {
      throw new UnsupportedOperationException(
          "There shouldn't be any language-level read nodes for internal slots. "
              + "They are used directly by other nodes.");
    }
  }
}
