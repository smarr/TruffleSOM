/**
 * Copyright (c) 2013 Stefan Marr,   stefan.marr@vub.ac.be
 * Copyright (c) 2009 Michael Haupt, michael.haupt@hpi.uni-potsdam.de
 * Software Architecture Group, Hasso Plattner Institute, Potsdam, Germany
 * http://www.hpi.uni-potsdam.de/swa/
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

package trufflesom.compiler;

import java.util.Collection;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import trufflesom.vm.Universe;
import trufflesom.vm.VmSettings;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SInvokable.SPrimitive;


public final class Disassembler {

  @TruffleBoundary
  public static void dump(final SClass cl) {
    Collection<SInvokable> invokables = cl.getInstanceInvokablesForDisassembler();
    if (invokables == null) {
      return;
    }

    for (SInvokable inv : invokables) {
      // output header and skip if the Invokable is a Primitive
      Universe.errorPrint(cl.getName().toString() + ">>"
          + inv.getSignature().toString() + " = ");

      if (inv instanceof SPrimitive) {
        Universe.errorPrintln("<primitive>");
        continue;
      }

      if (VmSettings.UseAstInterp) {
        dumpMethod(inv, "\t");
      } else {
        trufflesom.compiler.bc.Disassembler.dumpMethod((SMethod) inv, "\t");
      }
    }
  }

  @TruffleBoundary
  public static void dumpMethod(final SInvokable m, final String indent) {
    Universe.errorPrintln("(");
    Universe.errorPrintln(m.getInvokable().toString());
    Universe.errorPrintln(indent + ")");
  }

}
