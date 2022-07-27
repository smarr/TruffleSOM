package trufflesom.compiler;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;
import static trufflesom.compiler.bc.BytecodeGenerator.emitPOPARGUMENT;
import static trufflesom.compiler.bc.BytecodeGenerator.emitPOPLOCAL;
import static trufflesom.compiler.bc.BytecodeGenerator.emitPUSHARGUMENT;
import static trufflesom.compiler.bc.BytecodeGenerator.emitPUSHLOCAL;
import static trufflesom.vm.SymbolTable.symBlockSelf;
import static trufflesom.vm.SymbolTable.symSelf;
import static trufflesom.vm.SymbolTable.symbolFor;

import java.util.Objects;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.Source;

import bdt.source.SourceCoordinate;
import trufflesom.compiler.bc.BytecodeMethodGenContext;
import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgumentReadNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgumentWriteNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.NonLocalArgumentReadNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.NonLocalArgumentWriteNode;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.LocalVariableNode.LocalVariableReadNode;
import trufflesom.interpreter.nodes.LocalVariableNodeFactory.LocalVariableReadNodeGen;
import trufflesom.interpreter.nodes.LocalVariableNodeFactory.LocalVariableWriteNodeGen;
import trufflesom.interpreter.nodes.NonLocalVariableNode.NonLocalVariableReadNode;
import trufflesom.interpreter.nodes.NonLocalVariableNodeFactory.NonLocalVariableReadNodeGen;
import trufflesom.interpreter.nodes.NonLocalVariableNodeFactory.NonLocalVariableWriteNodeGen;
import trufflesom.interpreter.supernodes.IncLocalVariableNodeGen;
import trufflesom.interpreter.supernodes.IncNonLocalVariableNodeGen;
import trufflesom.interpreter.supernodes.IntIncLocalVariableNodeGen;
import trufflesom.interpreter.supernodes.IntIncNonLocalVariableNodeGen;
import trufflesom.interpreter.supernodes.LocalVariableReadSquareWriteNodeGen;
import trufflesom.interpreter.supernodes.LocalVariableSquareNodeGen;
import trufflesom.interpreter.supernodes.NonLocalVariableReadSquareWriteNodeGen;
import trufflesom.interpreter.supernodes.NonLocalVariableSquareNodeGen;
import trufflesom.primitives.arithmetic.AdditionPrim;
import trufflesom.vm.NotYetImplementedException;
import trufflesom.vmobjects.SSymbol;


public abstract class Variable implements bdt.inlining.Variable<ExpressionNode> {
  public final SSymbol name;
  public final long    coord;

  Variable(final SSymbol name, final long coord) {
    this.name = name;
    this.coord = coord;
  }

  public final SSymbol getName() {
    return name;
  }

  /** Gets the name including lexical location. */
  public final SSymbol getQualifiedName(final Source source) {
    return symbolFor(name.getString() + SourceCoordinate.getLocationQualifier(source, coord));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + name + ")";
  }

  @Override
  public abstract ExpressionNode getReadNode(int contextLevel, long coord);

  protected abstract void emitPop(BytecodeMethodGenContext mgenc);

  protected abstract void emitPush(BytecodeMethodGenContext mgenc);

  public abstract Variable split();

  public abstract Local splitToMergeIntoOuterScope(int slotIndex);

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
    if (var.coord == coord) {
      assert name == var.name : "Defined in the same place, but names not equal?";
      return true;
    }
    assert coord == 0
        || coord != var.coord : "Why are there multiple objects for this source section? might need to fix comparison above";
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, coord);
  }

  public static final class Argument extends Variable {
    public final int index;

    Argument(final SSymbol name, final int index, final long coord) {
      super(name, coord);
      this.index = index;
    }

    public boolean isSelf() {
      return symSelf == name || symBlockSelf == name;
    }

    @Override
    public Variable split() {
      return this;
    }

    @Override
    public Local splitToMergeIntoOuterScope(final int slotIndex) {
      if (isSelf()) {
        return null;
      }

      return new Local(name, coord, slotIndex);
    }

    @Override
    public ExpressionNode getReadNode(final int contextLevel, final long coord) {
      transferToInterpreterAndInvalidate();

      if (contextLevel == 0) {
        return new LocalArgumentReadNode(this).initialize(coord);
      } else {
        return new NonLocalArgumentReadNode(this, contextLevel).initialize(coord);
      }
    }

    @Override
    public ExpressionNode getIncNode(final int contextLevel, final long incValue,
        final long coord) {
      throw new NotYetImplementedException();
    }

    @Override
    public ExpressionNode getSquareNode(final int contextLevel, final long coord) {
      throw new NotYetImplementedException();
    }

    @Override
    public ExpressionNode getReadSquareWriteNode(final int contextLevel, final long coord,
        final Local readLocal) {
      throw new NotYetImplementedException();
    }

    @Override
    public ExpressionNode getWriteNode(final int contextLevel,
        final ExpressionNode valueExpr, final long coord) {
      transferToInterpreterAndInvalidate();

      if (contextLevel == 0) {
        return new LocalArgumentWriteNode(this, valueExpr).initialize(coord);
      } else {
        return new NonLocalArgumentWriteNode(this, contextLevel, valueExpr).initialize(coord);
      }
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

  public static class Local extends Variable {
    protected final int slotIndex;

    @CompilationFinal private FrameDescriptor descriptor;

    Local(final SSymbol name, final long coord, final int index) {
      super(name, coord);
      this.slotIndex = index;
    }

    Local(final SSymbol name, final long coord, final FrameDescriptor descriptor,
        final int index) {
      super(name, coord);
      this.slotIndex = index;
      this.descriptor = descriptor;
    }

    public void init(final FrameDescriptor descriptor) {
      this.descriptor = descriptor;
    }

    @Override
    public ExpressionNode getReadNode(final int contextLevel, final long coord) {
      transferToInterpreterAndInvalidate();
      if (contextLevel > 0) {
        return NonLocalVariableReadNodeGen.create(contextLevel, this).initialize(coord);
      }
      return LocalVariableReadNodeGen.create(this).initialize(coord);
    }

    @Override
    public ExpressionNode getIncNode(final int contextLevel, final long incValue,
        final long coord) {
      transferToInterpreterAndInvalidate();
      if (contextLevel > 0) {
        return IntIncNonLocalVariableNodeGen.create(contextLevel, this, incValue)
                                            .initialize(coord);
      }
      return IntIncLocalVariableNodeGen.create(this, incValue).initialize(coord);
    }

    @Override
    public ExpressionNode getSquareNode(final int contextLevel, final long coord) {
      transferToInterpreterAndInvalidate();
      if (contextLevel > 0) {
        return NonLocalVariableSquareNodeGen.create(contextLevel, this).initialize(coord);
      }
      return LocalVariableSquareNodeGen.create(this).initialize(coord);
    }

    @Override
    public ExpressionNode getReadSquareWriteNode(final int contextLevel, final long coord,
        final Local readLocal) {
      if (contextLevel > 0) {
        return NonLocalVariableReadSquareWriteNodeGen.create(contextLevel, this, readLocal)
                                                     .initialize(coord);
      }
      return LocalVariableReadSquareWriteNodeGen.create(this, readLocal).initialize(coord);
    }

    public final int getIndex() {
      return slotIndex;
    }

    @Override
    public Local split() {
      return new Local(name, coord, slotIndex);
    }

    @Override
    public Local splitToMergeIntoOuterScope(final int slotIndex) {
      return new Local(name, coord, slotIndex);
    }

    @Override
    public ExpressionNode getWriteNode(final int contextLevel,
        final ExpressionNode valueExpr, final long coord) {
      transferToInterpreterAndInvalidate();
      if (contextLevel > 0) {
        if (valueExpr instanceof AdditionPrim) {
          AdditionPrim add = (AdditionPrim) valueExpr;
          ExpressionNode rcvr = add.getReceiver();
          ExpressionNode arg = add.getArgument();

          if (rcvr instanceof NonLocalVariableReadNode
              && ((NonLocalVariableReadNode) rcvr).getLocal() == this) {
            return IncNonLocalVariableNodeGen.create(contextLevel, this, arg)
                                             .initialize(coord);
          }

          if (arg instanceof NonLocalVariableReadNode
              && ((NonLocalVariableReadNode) arg).getLocal() == this) {
            return IncNonLocalVariableNodeGen.create(contextLevel, this, rcvr)
                                             .initialize(coord);
          }
        }

        return NonLocalVariableWriteNodeGen.create(contextLevel, this, valueExpr)
                                           .initialize(coord);
      }

      if (valueExpr instanceof AdditionPrim) {
        AdditionPrim add = (AdditionPrim) valueExpr;
        ExpressionNode rcvr = add.getReceiver();
        ExpressionNode arg = add.getArgument();

        if (rcvr instanceof LocalVariableReadNode
            && ((LocalVariableReadNode) rcvr).getLocal() == this) {
          return IncLocalVariableNodeGen.create(this, arg)
                                        .initialize(coord);
        }

        if (arg instanceof LocalVariableReadNode
            && ((LocalVariableReadNode) arg).getLocal() == this) {
          return IncLocalVariableNodeGen.create(this, rcvr)
                                        .initialize(coord);
        }
      }

      return LocalVariableWriteNodeGen.create(this, valueExpr).initialize(coord);
    }

    public final FrameDescriptor getFrameDescriptor() {
      assert descriptor != null : "Locals need to be initialized with a frame descriptior. Call init(.) first!";
      return descriptor;
    }

    @Override
    public void emitPop(final BytecodeMethodGenContext mgenc) {
      int contextLevel = mgenc.getContextLevel(this);
      emitPOPLOCAL(mgenc, (byte) slotIndex, (byte) contextLevel);
    }

    @Override
    public void emitPush(final BytecodeMethodGenContext mgenc) {
      int contextLevel = mgenc.getContextLevel(this);
      emitPUSHLOCAL(mgenc, (byte) slotIndex, (byte) contextLevel);
    }
  }

  public static final class Internal extends Local {
    public Internal(final SSymbol name, final long coord, final int slotIndex) {
      super(name, coord, slotIndex);
    }

    public Internal(final SSymbol name, final long coord,
        final FrameDescriptor descriptor, final int slotIndex) {
      super(name, coord, descriptor, slotIndex);
    }

    @Override
    public ExpressionNode getReadNode(final int contextLevel, final long coord) {
      throw new UnsupportedOperationException(
          "There shouldn't be any language-level read nodes for internal slots. "
              + "They are used directly by other nodes.");
    }

    @Override
    public ExpressionNode getIncNode(final int contextLevel, final long incValue,
        final long coord) {
      throw new UnsupportedOperationException(
          "There shouldn't be any language-level inc nodes for internal slots. "
              + "They are used directly by other nodes.");
    }

    @Override
    public ExpressionNode getSquareNode(final int contextLevel, final long coord) {
      throw new UnsupportedOperationException(
          "There shouldn't be any language-level square nodes for internal slots. ");
    }

    @Override
    public ExpressionNode getReadSquareWriteNode(final int contextLevel, final long coord,
        final Local readLocal) {
      throw new UnsupportedOperationException(
          "There shouldn't be any language-level square nodes for internal slots. ");
    }

    @Override
    public Internal split() {
      return new Internal(name, coord, slotIndex);
    }

    @Override
    public Local splitToMergeIntoOuterScope(final int slotIndex) {
      return null;
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
      return var.coord == coord && name == var.name;
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, coord);
    }
  }
}
