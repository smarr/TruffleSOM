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
import som.vmobjects.SAbstractObject;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

public final class GlobalNode extends ExpressionNode {
  private   final Universe universe;
  protected final SSymbol  globalName;

  public GlobalNode(final SSymbol globalName, final SourceSection source) {
    super(source);
    this.globalName = globalName;
    this.universe   = Universe.current();
  }

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

  protected Object executeUnknownGlobal(final VirtualFrame frame) {
    Object self = SArguments.rcvr(frame);
    return SAbstractObject.sendUnknownGlobal(self, globalName);
  }
}
