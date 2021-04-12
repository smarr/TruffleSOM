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

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import bd.primitives.nodes.PreevaluatedExpression;
import bd.tools.nodes.Invocation;
import trufflesom.interpreter.SArguments;
import trufflesom.interpreter.TruffleCompiler;
import trufflesom.vm.Universe;
import trufflesom.vm.Universe.Association;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SAbstractObject;
import trufflesom.vmobjects.SSymbol;


public abstract class GlobalNode extends ExpressionNode
    implements Invocation<SSymbol>, PreevaluatedExpression {

  public static GlobalNode create(final SSymbol globalName, final Universe universe,
      final SourceSection source) {
    if (globalName == universe.symNil) {
      return new NilGlobalNode(globalName).initialize(source);
    } else if (globalName.getString().equals("true")) {
      return new TrueGlobalNode(globalName).initialize(source);
    } else if (globalName.getString().equals("false")) {
      return new FalseGlobalNode(globalName).initialize(source);
    }

    // Get the global from the universe
    Association assoc = universe.getGlobalsAssociation(globalName);
    if (assoc != null) {
      return new CachedGlobalReadNode(globalName, assoc, source);
    }
    return new UninitializedGlobalReadNode(globalName, source, universe);
  }

  protected final SSymbol globalName;

  public GlobalNode(final SSymbol globalName) {
    this.globalName = globalName;
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

  private abstract static class AbstractUninitializedGlobalReadNode extends GlobalNode {
    protected final Universe universe;

    AbstractUninitializedGlobalReadNode(final SSymbol globalName,
        final SourceSection source, final Universe universe) {
      super(globalName);
      this.universe = universe;
      initialize(source);
    }

    protected abstract Object executeUnknownGlobal(VirtualFrame frame);

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      TruffleCompiler.transferToInterpreterAndInvalidate("Uninitialized Global Node");

      // Get the global from the universe
      Association assoc = universe.getGlobalsAssociation(globalName);
      if (assoc != null) {
        return replace((GlobalNode) new CachedGlobalReadNode(
            globalName, assoc, sourceSection)).executeGeneric(frame);
      } else {
        return executeUnknownGlobal(frame);
      }
    }
  }

  private static final class UninitializedGlobalReadNode
      extends AbstractUninitializedGlobalReadNode {
    UninitializedGlobalReadNode(final SSymbol globalName, final SourceSection source,
        final Universe universe) {
      super(globalName, source, universe);
    }

    @Override
    protected Object executeUnknownGlobal(final VirtualFrame frame) {
      CompilerAsserts.neverPartOfCompilation();

      // if it is not defined, we will send a error message to the current
      // receiver object
      Object self = SArguments.rcvr(frame);
      return SAbstractObject.sendUnknownGlobal(self, globalName, universe);
    }
  }

  public static final class UninitializedGlobalReadWithoutErrorNode
      extends AbstractUninitializedGlobalReadNode {
    public UninitializedGlobalReadWithoutErrorNode(final SSymbol globalName,
        final SourceSection source, final Universe universe) {
      super(globalName, source, universe);
    }

    @Override
    protected Object executeUnknownGlobal(final VirtualFrame frame) {
      return Nil.nilObject;
    }
  }

  private static final class CachedGlobalReadNode extends GlobalNode {
    private final Association            assoc;
    @CompilationFinal private Assumption assumption;

    private CachedGlobalReadNode(final SSymbol globalName, final Association assoc,
        final SourceSection source) {
      super(globalName);
      this.assoc = assoc;
      this.assumption = assoc.getAssumption();
      initialize(source);
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

  private static final class TrueGlobalNode extends GlobalNode {
    TrueGlobalNode(final SSymbol globalName) {
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
  }

  private static final class FalseGlobalNode extends GlobalNode {
    FalseGlobalNode(final SSymbol globalName) {
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
  }

  private static final class NilGlobalNode extends GlobalNode {
    NilGlobalNode(final SSymbol globalName) {
      super(globalName);
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      return Nil.nilObject;
    }
  }
}
