/**
 * Copyright (c) 2013 Stefan Marr, stefan.marr@vub.ac.be
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package trufflesom.interpreter.nodes;

import static trufflesom.vm.Globals.getGlobalsAssociation;
import static trufflesom.vm.Globals.hasGlobal;
import static trufflesom.vm.SymbolTable.symFalse;
import static trufflesom.vm.SymbolTable.symNil;
import static trufflesom.vm.SymbolTable.symTrue;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.source.Source;

import bd.inlining.ScopeAdaptationVisitor;
import bd.primitives.nodes.PreevaluatedExpression;
import bd.tools.nodes.Invocation;
import trufflesom.compiler.MethodGenerationContext;
import trufflesom.compiler.Parser.ParseError;
import trufflesom.compiler.bc.BytecodeGenerator;
import trufflesom.compiler.bc.BytecodeMethodGenContext;
import trufflesom.interpreter.TruffleCompiler;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.CachedLiteralNode;
import trufflesom.interpreter.nodes.dispatch.DispatchGuard;
import trufflesom.vm.Globals.Association;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SAbstractObject;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SSymbol;


@GenerateWrapper
public abstract class GlobalNode extends ExpressionNode
    implements Invocation<SSymbol>, PreevaluatedExpression {

  public static boolean isPotentiallyUnknown(final SSymbol global) {
    return global != symNil && global != symTrue
        && global != symFalse && !hasGlobal(global);
  }

  public static GlobalNode create(final SSymbol globalName,
      final MethodGenerationContext mgenc) {
    if (globalName == symNil) {
      return new NilGlobalNode(globalName);
    } else if (globalName == symTrue) {
      return new TrueGlobalNode(globalName);
    } else if (globalName == symFalse) {
      return new FalseGlobalNode(globalName);
    }

    // Get the global from the universe
    Association assoc = getGlobalsAssociation(globalName);
    if (assoc != null) {
      return new CachedGlobalReadNode(globalName, assoc);
    }

    if (mgenc != null) {
      mgenc.markAccessingOuterScopes();
    }
    return new UninitializedGlobalReadNode(globalName);
  }

  public static Object sendUnknownGlobalToMethodRcvr(Object self, final SSymbol globalName) {
    while (self instanceof SBlock) {
      self = ((SBlock) self).getOuterSelf();
    }

    // if it is not defined, we will send a error message to the current
    // receiver object

    return SAbstractObject.sendUnknownGlobal(self, globalName);
  }

  protected final SSymbol globalName;

  public GlobalNode(final SSymbol globalName) {
    this.globalName = globalName;
  }

  protected GlobalNode() {
    this.globalName = null;
  }

  @Override
  public WrapperNode createWrapper(final ProbeNode probe) {
    return new GlobalNodeWrapper(this, probe);
  }

  @Override
  public final SSymbol getInvocationIdentifier() {
    return globalName;
  }

  @Override
  public boolean isTrivial() {
    return true;
  }

  @Override
  public PreevaluatedExpression copyTrivialNode() {
    return (PreevaluatedExpression) copy();
  }

  @Override
  public Object doPreEvaluated(final VirtualFrame frame, final Object[] args) {
    return executeGeneric(frame);
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() + "(" + globalName.getString() + ")";
  }

  private abstract static class AbstractUninitializedGlobalReadNode extends GlobalNode {

    AbstractUninitializedGlobalReadNode(final SSymbol globalName) {
      super(globalName);
    }

    protected abstract Object executeUnknownGlobal(VirtualFrame frame);

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      TruffleCompiler.transferToInterpreterAndInvalidate("Uninitialized Global Node");

      // Get the global from the universe
      Association assoc = getGlobalsAssociation(globalName);
      if (assoc != null) {
        return replace(
            (GlobalNode) new CachedGlobalReadNode(globalName, assoc)).executeGeneric(frame);
      } else {
        return executeUnknownGlobal(frame);
      }
    }
  }

  public static final class UninitializedGlobalReadNode
      extends AbstractUninitializedGlobalReadNode {
    UninitializedGlobalReadNode(final SSymbol globalName) {
      super(globalName);
    }

    @Override
    protected Object executeUnknownGlobal(final VirtualFrame frame) {
      CompilerAsserts.neverPartOfCompilation();

      // find outer self
      Object self = frame.getArguments()[0];
      return sendUnknownGlobalToMethodRcvr(self, globalName);
    }

    @Override
    public boolean isTrivial() {
      return true;
    }

    @Override
    public boolean isTrivialInBlock() {
      return false;
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      Object scope = inliner.getCurrentScope();

      if (scope instanceof BytecodeMethodGenContext) {
        BytecodeMethodGenContext mgenc = (BytecodeMethodGenContext) scope;
        try {
          BytecodeGenerator.emitPUSHGLOBAL(mgenc, globalName, null);
        } catch (ParseError e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  public static final class UninitializedGlobalReadWithoutErrorNode
      extends AbstractUninitializedGlobalReadNode {
    public UninitializedGlobalReadWithoutErrorNode(final SSymbol globalName) {
      super(globalName);
    }

    @Override
    protected Object executeUnknownGlobal(final VirtualFrame frame) {
      return Nil.nilObject;
    }
  }

  private static final class CachedGlobalReadNode extends GlobalNode {
    private final Association            assoc;
    @CompilationFinal private Assumption assumption;

    private CachedGlobalReadNode(final SSymbol globalName, final Association assoc) {
      super(globalName);
      this.assoc = assoc;
      this.assumption = assoc.getAssumption();
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      if (!assumption.isValid()) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        assumption = assoc.getAssumption();
      }
      return assoc.getValue();
    }
  }

  public static final class TrueGlobalNode extends GlobalNode {
    public TrueGlobalNode(final SSymbol globalName) {
      super(globalName);
    }

    @Override
    public boolean executeBoolean(final VirtualFrame frame) {
      return true;
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      return executeBoolean(frame);
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      Object scope = inliner.getCurrentScope();

      if (scope instanceof BytecodeMethodGenContext) {
        BytecodeMethodGenContext mgenc = (BytecodeMethodGenContext) scope;
        try {
          BytecodeGenerator.emitPUSHCONSTANT(mgenc, true, null);
        } catch (ParseError e) {
          throw new RuntimeException(e);
        }
      }
    }

    @Override
    public AbstractDispatchNode asDispatchNode(final Object rcvr, final Source source,
        final AbstractDispatchNode next) {
      return new CachedLiteralNode(DispatchGuard.create(rcvr), source, true, next);
    }
  }

  public static final class FalseGlobalNode extends GlobalNode {
    public FalseGlobalNode(final SSymbol globalName) {
      super(globalName);
    }

    @Override
    public boolean executeBoolean(final VirtualFrame frame) {
      return false;
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      return executeBoolean(frame);
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      Object scope = inliner.getCurrentScope();

      if (scope instanceof BytecodeMethodGenContext) {
        BytecodeMethodGenContext mgenc = (BytecodeMethodGenContext) scope;
        try {
          BytecodeGenerator.emitPUSHCONSTANT(mgenc, false, null);
        } catch (ParseError e) {
          throw new RuntimeException(e);
        }
      }
    }

    @Override
    public AbstractDispatchNode asDispatchNode(final Object rcvr, final Source source,
        final AbstractDispatchNode next) {
      return new CachedLiteralNode(DispatchGuard.create(rcvr), source, false, next);
    }
  }

  public static final class NilGlobalNode extends GlobalNode {
    public NilGlobalNode(final SSymbol globalName) {
      super(globalName);
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      return Nil.nilObject;
    }

    @Override
    public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
      Object scope = inliner.getCurrentScope();

      if (scope instanceof BytecodeMethodGenContext) {
        BytecodeMethodGenContext mgenc = (BytecodeMethodGenContext) scope;
        try {
          BytecodeGenerator.emitPUSHCONSTANT(mgenc, Nil.nilObject, null);
        } catch (ParseError e) {
          throw new RuntimeException(e);
        }
      }
    }

    @Override
    public AbstractDispatchNode asDispatchNode(final Object rcvr, final Source source,
        final AbstractDispatchNode next) {
      return new CachedLiteralNode(DispatchGuard.create(rcvr), source, Nil.nilObject, next);
    }
  }
}
