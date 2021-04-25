package trufflesom.compiler;

import static trufflesom.compiler.bc.BytecodeGenerator.emitPOPARGUMENT;
import static trufflesom.compiler.bc.BytecodeGenerator.emitPOPLOCAL;
import static trufflesom.compiler.bc.BytecodeGenerator.emitPUSHARGUMENT;
import static trufflesom.compiler.bc.BytecodeGenerator.emitPUSHLOCAL;
import static trufflesom.interpreter.SNodeFactory.createArgumentRead;
import static trufflesom.interpreter.SNodeFactory.createArgumentWrite;
import static trufflesom.interpreter.SNodeFactory.createLocalVarRead;
import static trufflesom.interpreter.SNodeFactory.createVariableWrite;
import static trufflesom.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;

import java.util.Objects;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.source.SourceSection;

import bd.inlining.NodeState;
import trufflesom.compiler.bc.BytecodeMethodGenContext;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.vm.NotYetImplementedException;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SSymbol;


public abstract class Variable implements bd.inlining.Variable<ExpressionNode> {
  public final SSymbol       name;
  public final SourceSection source;

  Variable(final SSymbol name, final SourceSection source) {
    this.name = name;
    this.source = source;
  }

  public final SSymbol getName() {
    return name;
  }

  /** Gets the name including lexical location. */
  public final SSymbol getQualifiedName(final Universe u) {
    return u.symbolFor(name.getString() + Universe.getLocationQualifier(source));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + name + ")";
  }

  @Override
  public abstract ExpressionNode getReadNode(int contextLevel, SourceSection source);

  protected abstract void emitPop(BytecodeMethodGenContext mgenc);

  protected abstract void emitPush(BytecodeMethodGenContext mgenc);

  public abstract Variable split(FrameDescriptor descriptor);

  public abstract Local splitToMergeIntoOuterScope(Universe universe,
      FrameDescriptor descriptor);

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

  @Override
  public int hashCode() {
    return Objects.hash(name, source);
  }

  public static final class AccessNodeState implements NodeState {
    private final SSymbol holderClass;
    private final boolean classSide;

    public AccessNodeState(final SSymbol holderClass, final boolean classSide) {
      this.holderClass = holderClass;
      this.classSide = classSide;
    }
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

    @Override
    public Variable split(final FrameDescriptor descriptor) {
      return this;
    }

    @Override
    public Local splitToMergeIntoOuterScope(final Universe universe,
        final FrameDescriptor descriptor) {
      if (isSelf(universe)) {
        return null;
      }

      Local l = new Local(name, source);
      l.init(descriptor.addFrameSlot(l), descriptor);
      return l;
    }

    @Override
    public ExpressionNode getReadNode(final int contextLevel,
        final SourceSection source) {
      transferToInterpreterAndInvalidate("Variable.getReadNode");
      return createArgumentRead(this, contextLevel, source);
    }

    @Override
    public ExpressionNode getSuperReadNode(final int contextLevel, final NodeState state,
        final SourceSection source) {
      throw new UnsupportedOperationException("Not needed in this implementation");
    }

    @Override
    public ExpressionNode getWriteNode(final int contextLevel,
        final ExpressionNode valueExpr, final SourceSection source) {
      transferToInterpreterAndInvalidate("Variable.getWriteNode");
      return createArgumentWrite(this, contextLevel, valueExpr, source);
    }

    @Override
    public void emitPop(final BytecodeMethodGenContext mgenc) {
      emitPOPARGUMENT(mgenc, (byte) index, (byte) mgenc.getContextLevel(this));
    }

    @Override
    protected void emitPush(final BytecodeMethodGenContext mgenc) {
      emitPUSHARGUMENT(mgenc, (byte) index, (byte) mgenc.getContextLevel(this));
    }
  }

  public static final class Local extends Variable {
    @CompilationFinal private transient FrameSlot slot;
    @CompilationFinal private FrameDescriptor     descriptor;

    Local(final SSymbol name, final SourceSection source) {
      super(name, source);
    }

    public void init(final FrameSlot slot, final FrameDescriptor descriptor) {
      this.slot = slot;
      this.descriptor = descriptor;
    }

    @Override
    public ExpressionNode getReadNode(final int contextLevel, final SourceSection source) {
      transferToInterpreterAndInvalidate("Variable.getReadNode");
      return createLocalVarRead(this, contextLevel, source);
    }

    public FrameSlot getSlot() {
      return slot;
    }

    @Override
    public Local split(final FrameDescriptor descriptor) {
      Local newLocal = new Local(name, source);
      newLocal.init(descriptor.addFrameSlot(newLocal), descriptor);
      return newLocal;
    }

    @Override
    public Local splitToMergeIntoOuterScope(final Universe universe,
        final FrameDescriptor descriptor) {
      return split(descriptor);
    }

    @Override
    public ExpressionNode getWriteNode(final int contextLevel,
        final ExpressionNode valueExpr, final SourceSection source) {
      transferToInterpreterAndInvalidate("Variable.getWriteNode");
      return createVariableWrite(this, contextLevel, valueExpr, source);
    }

    public FrameDescriptor getFrameDescriptor() {
      return descriptor;
    }

    @Override
    public void emitPop(final BytecodeMethodGenContext mgenc) {
      int contextLevel = mgenc.getContextLevel(this);
      emitPOPLOCAL(mgenc, mgenc.getLocalIndex(this, contextLevel), (byte) contextLevel);
    }

    @Override
    public void emitPush(final BytecodeMethodGenContext mgenc) {
      int contextLevel = mgenc.getContextLevel(this);
      emitPUSHLOCAL(mgenc, mgenc.getLocalIndex(this, contextLevel), (byte) contextLevel);
    }
  }

  public static final class Internal extends Variable {
    @CompilationFinal private FrameSlot       slot;
    @CompilationFinal private FrameDescriptor descriptor;

    public Internal(final SSymbol name, final SourceSection source) {
      super(name, source);
    }

    public void init(final FrameSlot slot, final FrameDescriptor descriptor) {
      assert this.slot == null && slot != null;

      this.slot = slot;
      this.descriptor = descriptor;
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

    @Override
    public Variable split(final FrameDescriptor descriptor) {
      Internal newInternal = new Internal(name, source);

      assert this.descriptor.getFrameSlotKind(
          slot) == FrameSlotKind.Object : "We only have the on stack marker currently, so, we expect those not to specialize";
      newInternal.init(descriptor.addFrameSlot(newInternal, FrameSlotKind.Object), descriptor);
      return newInternal;
    }

    @Override
    public Local splitToMergeIntoOuterScope(final Universe universe,
        final FrameDescriptor descriptor) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void emitPop(final BytecodeMethodGenContext mgenc) {
      throw new NotYetImplementedException();
    }

    @Override
    public void emitPush(final BytecodeMethodGenContext mgenc) {
      throw new NotYetImplementedException();
    }

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
      return var.source == source && name == var.name;
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, source);
    }
  }
}
