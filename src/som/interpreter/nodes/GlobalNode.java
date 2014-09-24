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

import som.interpreter.SArguments;
import som.vm.Universe;
import som.vm.Universe.Association;
import som.vm.constants.Nil;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;


public abstract class GlobalNode extends ExpressionNode {

  protected final SSymbol  globalName;


  public GlobalNode(final SSymbol globalName, final SourceSection source) {
    super(source);
    this.globalName = globalName;
  }

  public abstract static class AbstractUninitializedGlobalReadNode extends GlobalNode {
    private final Universe universe;

    public AbstractUninitializedGlobalReadNode(final SSymbol globalName,
        final Universe universe, final SourceSection source) {
      super(globalName, source);
      this.universe = universe;
    }

    protected abstract Object executeUnknownGlobal(VirtualFrame frame);

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      // Get the global from the universe
      Association assoc = universe.getGlobalsAssociation(globalName);
      if (assoc != null) {
        return assoc.getValue();
      } else {
        return executeUnknownGlobal(frame);
      }
    }
  }

  public static final class UninitializedGlobalReadNode extends AbstractUninitializedGlobalReadNode {

    public UninitializedGlobalReadNode(final SSymbol globalName,
        final SourceSection source) {
      super(globalName, Universe.current(), source);
    }

    @Override
    protected Object executeUnknownGlobal(final VirtualFrame frame) {
      // if it is not defined, we will send a error message to the current
      // receiver object
      Object self = SArguments.rcvr(frame);
      return SAbstractObject.sendUnknownGlobal(self, globalName);
    }
  }

  public static final class UninitializedGlobalReadWithoutErrorNode extends AbstractUninitializedGlobalReadNode {
    public UninitializedGlobalReadWithoutErrorNode(final SSymbol globalName,
        final SourceSection source) {
      super(globalName, Universe.current(), source);
    }

    @Override
    protected Object executeUnknownGlobal(final VirtualFrame frame) {
      return Nil.nilObject;
    }
  }
}
