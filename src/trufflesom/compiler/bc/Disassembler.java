/**
 * Copyright (c) 2017 Michael Haupt, github@haupz.de
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

package trufflesom.compiler.bc;

import static trufflesom.interpreter.bc.Bytecodes.POP_ARGUMENT;
import static trufflesom.interpreter.bc.Bytecodes.POP_FIELD;
import static trufflesom.interpreter.bc.Bytecodes.POP_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_ARGUMENT;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_BLOCK;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_CONSTANT;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_FIELD;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_GLOBAL;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.SEND;
import static trufflesom.interpreter.bc.Bytecodes.SUPER_SEND;
import static trufflesom.interpreter.bc.Bytecodes.getBytecodeLength;
import static trufflesom.interpreter.bc.Bytecodes.getPaddedBytecodeName;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

import trufflesom.interpreter.Invokable;
import trufflesom.interpreter.SomLanguage;
import trufflesom.interpreter.Types;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.SOMNode;
import trufflesom.interpreter.nodes.bc.BytecodeLoopNode;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SInvokable.SPrimitive;
import trufflesom.vmobjects.SSymbol;


public class Disassembler {

  public static void dump(final SClass cl) {
    for (int i = 0; i < cl.getNumberOfInstanceInvokables(); i++) {
      SInvokable inv = cl.getInstanceInvokable(i);

      // output header and skip if the Invokable is a Primitive
      Universe.errorPrint(cl.getName().toString() + ">>"
          + inv.getSignature().toString() + " = ");

      if (inv instanceof SPrimitive) {
        Universe.errorPrintln("<primitive>");
        continue;
      }
      // output actual method
      dumpMethod((SMethod) inv, "\t");
    }
  }

  private static BytecodeLoopNode getBytecodeNode(final SMethod m) {
    RootNode i = m.getInvokable();
    return getBytecodeNode(i);
  }

  private static BytecodeLoopNode getBytecodeNode(final RootNode i) {
    for (Node n : i.getChildren()) {
      if (n instanceof SOMNode) {
        ExpressionNode e = ((SOMNode) n).getFirstMethodBodyNode();
        if (e instanceof BytecodeLoopNode) {
          return (BytecodeLoopNode) e;
        }
      }
    }

    return null;
  }

  public static void dumpMethod(final SMethod method) {
    dumpMethod(method, "");
  }

  public static void dumpMethod(final RootNode method) {
    dumpMethod(method, "");
  }

  public static void dumpMethod(final SMethod method, final String indent) {
    BytecodeLoopNode m = getBytecodeNode(method);
    dumpMethod(m, indent);
  }

  public static void dumpMethod(final RootNode method, final String indent) {
    dumpMethod(getBytecodeNode(method), indent);
  }

  private static SClass getClass(final BytecodeLoopNode m) {
    Invokable i = (Invokable) m.getRootNode();
    return i.getHolder();
  }

  public static void dumpMethod(final BytecodeLoopNode m, final String indent) {
    SClass clazz = getClass(m);
    Universe u = SomLanguage.getCurrent().getUniverse();

    Universe.errorPrintln("(");

    // output stack information
    Universe.errorPrintln(indent + "<" + m.getNumberOfLocals() + " locals, "
        + m.getMaximumNumberOfStackElements() + " stack, "
        + m.getNumberOfBytecodes() + " bc_count>");

    // output bytecodes
    for (int b = 0; b < m.getNumberOfBytecodes(); b +=
        getBytecodeLength(m.getBytecode(b))) {

      Universe.errorPrint(indent);

      // bytecode index
      if (b < 10) {
        Universe.errorPrint(" ");
      }
      if (b < 100) {
        Universe.errorPrint(" ");
      }
      Universe.errorPrint(" " + b + ":");

      // mnemonic
      byte bytecode = m.getBytecode(b);
      Universe.errorPrint(getPaddedBytecodeName(bytecode) + "  ");

      // parameters (if any)
      if (getBytecodeLength(bytecode) == 1) {
        Universe.errorPrintln();
        continue;
      }
      switch (bytecode) {
        case PUSH_LOCAL: {
          Universe.errorPrintln("local: " + m.getBytecode(b + 1) + ", context: "
              + m.getBytecode(b + 2));
          break;
        }

        case PUSH_ARGUMENT: {
          Universe.errorPrintln("argument: " + m.getBytecode(b + 1) + ", context "
              + m.getBytecode(b + 2));
          break;
        }

        case PUSH_FIELD: {
          int idx = m.getBytecode(b + 1);
          String fieldName = ((SSymbol) clazz.getInstanceFields()
                                             .debugGetObject(idx)).getString();
          Universe.errorPrintln("(index: " + idx + ") field: " + fieldName);
          break;
        }

        case PUSH_BLOCK: {
          int idx = m.getBytecode(b + 1);
          Universe.errorPrint("block: (index: " + idx + ") ");
          dumpMethod((SMethod) m.getConstant(idx), indent + "\t");
          break;
        }

        case PUSH_CONSTANT: {
          int idx = m.getBytecode(b + 1);
          Object constant = m.getConstant(idx);
          SClass constantClass = Types.getClassOf(constant, u);
          Universe.errorPrintln("(index: " + idx + ") value: "
              + "("
              + constantClass.getName().toString()
              + ") "
              + constant.toString());
          break;
        }

        case PUSH_GLOBAL: {
          int idx = m.getBytecode(b + 1);
          Universe.errorPrintln("(index: " + idx + ") value: "
              + ((SSymbol) m.getConstant(idx)).toString());
          break;
        }

        case POP_LOCAL: {
          Universe.errorPrintln("local: " + m.getBytecode(b + 1) + ", context: "
              + m.getBytecode(b + 2));
          break;
        }

        case POP_ARGUMENT: {
          Universe.errorPrintln("argument: " + m.getBytecode(b + 1)
              + ", context: " + m.getBytecode(b + 2));
          break;
        }

        case POP_FIELD: {
          int idx = m.getBytecode(b + 1);
          String fieldName = ((SSymbol) clazz.getInstanceFields()
                                             .debugGetObject(idx)).getString();
          Universe.errorPrintln("(index: " + idx + ") field: " + fieldName);
          break;
        }

        case SEND: {
          int idx = m.getBytecode(b + 1);
          Universe.errorPrintln("(index: " + idx
              + ") signature: " + ((SSymbol) m.getConstant(idx)).toString());
          break;
        }

        case SUPER_SEND: {
          int idx = m.getBytecode(b + 1);
          Universe.errorPrintln("(index: " + idx
              + ") signature: " + ((SSymbol) m.getConstant(idx)).toString());
          break;
        }

        default:
          Universe.errorPrintln("<incorrect bytecode>");
      }
    }
    Universe.errorPrintln(indent + ")");
  }

}
