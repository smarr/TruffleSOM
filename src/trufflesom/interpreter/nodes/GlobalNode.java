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
package som.interpreter.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import som.interpreter.SArguments;
import som.interpreter.TruffleCompiler;
import som.vm.Universe;
import som.vm.Universe.Association;
import som.vm.constants.Nil;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SSymbol;


public abstract class GlobalNode extends ExpressionNode {

  protected final SSymbol globalName;

  public GlobalNode(final SSymbol globalName) {
    this.globalName = globalName;
  }

  public abstract static class AbstractUninitializedGlobalReadNode extends GlobalNode {
    protected final Universe universe;

    public AbstractUninitializedGlobalReadNode(final SSymbol globalName,
        final SourceSection source, final Universe universe) {
      super(globalName);
      this.universe = universe;
      initialize(source);
    }

    protected abstract Object executeUnknownGlobal(VirtualFrame frame);

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      TruffleCompiler.transferToInterpreterAndInvalidate("Uninitialized Global Node");

      // first let's check whether it is one of the well known globals
      switch (globalName.getString()) {
        case "true":
          return replace((GlobalNode) new TrueGlobalNode(
              globalName).initialize(sourceSection)).executeGeneric(frame);
        case "false":
          return replace((GlobalNode) new FalseGlobalNode(
              globalName).initialize(sourceSection)).executeGeneric(frame);
        case "nil":
          return replace((GlobalNode) new NilGlobalNode(
              globalName).initialize(sourceSection)).executeGeneric(frame);
      }

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

  public static final class UninitializedGlobalReadNode
      extends AbstractUninitializedGlobalReadNode {
    public UninitializedGlobalReadNode(final SSymbol globalName, final SourceSection source,
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
    private final Association assoc;

    private CachedGlobalReadNode(final SSymbol globalName, final Association assoc,
        final SourceSection source) {
      super(globalName);
      this.assoc = assoc;
      initialize(source);
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
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
