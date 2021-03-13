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

import static trufflesom.interpreter.bc.Bytecodes.INC_FIELD;
import static trufflesom.interpreter.bc.Bytecodes.INC_FIELD_PUSH;
import static trufflesom.interpreter.bc.Bytecodes.POP_ARGUMENT;
import static trufflesom.interpreter.bc.Bytecodes.POP_FIELD;
import static trufflesom.interpreter.bc.Bytecodes.POP_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_ARGUMENT;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_BLOCK;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_CONSTANT;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_FIELD;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_GLOBAL;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.Q_PUSH_GLOBAL;
import static trufflesom.interpreter.bc.Bytecodes.Q_SEND;
import static trufflesom.interpreter.bc.Bytecodes.Q_SEND_1;
import static trufflesom.interpreter.bc.Bytecodes.Q_SEND_2;
import static trufflesom.interpreter.bc.Bytecodes.Q_SEND_3;
import static trufflesom.interpreter.bc.Bytecodes.RETURN_NON_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.SEND;
import static trufflesom.interpreter.bc.Bytecodes.SUPER_SEND;
import static trufflesom.interpreter.bc.Bytecodes.getBytecodeLength;
import static trufflesom.interpreter.bc.Bytecodes.getPaddedBytecodeName;

import java.util.List;

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
    dumpMethod(m.getBytecodes(), indent, m.getNumberOfLocals(),
        m.getMaximumNumberOfStackElements(), clazz, m);
  }

  public static void dumpMethod(final List<Byte> bytecodes) {
    dumpMethod(bytecodes, "", 0, 0, null, null);
  }

  public static void dumpMethod(final List<Byte> bytecodes, final String indent,
      final int numLocals, final int maxStack, final SClass clazz, final BytecodeLoopNode m) {
    Universe u = SomLanguage.getCurrent().getUniverse();
    Universe.errorPrintln("(");

    // output stack information
    Universe.errorPrintln(indent + "<" + numLocals + " locals, "
        + maxStack + " stack, "
        + bytecodes.size() + " bc_count>");

    // output bytecodes
    for (int b = 0; b < bytecodes.size(); b +=
        getBytecodeLength(bytecodes.get(b))) {

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
      byte bytecode = bytecodes.get(b);
      Universe.errorPrint(getPaddedBytecodeName(bytecode) + "  ");

      // parameters (if any)
      if (getBytecodeLength(bytecode) == 1) {
        Universe.errorPrintln();
        continue;
      }
      switch (bytecode) {
        case POP_LOCAL:
        case PUSH_LOCAL: {
          Universe.errorPrintln("local: " + bytecodes.get(b + 1) + ", context: "
              + bytecodes.get(b + 2));
          break;
        }

        case POP_ARGUMENT:
        case PUSH_ARGUMENT: {
          Universe.errorPrintln("argument: " + bytecodes.get(b + 1) + ", context "
              + bytecodes.get(b + 2));
          break;
        }

        case INC_FIELD:
        case INC_FIELD_PUSH:
        case POP_FIELD:
        case PUSH_FIELD: {
          int idx = bytecodes.get(b + 1);
          int ctx = bytecodes.get(b + 2);

          Universe.errorPrint("(index: " + idx
              + ", context: " + ctx + ")");

          if (clazz != null) {
            String fieldName = ((SSymbol) clazz.getInstanceFields()
                                               .debugGetObject(idx)).getString();
            Universe.errorPrint(" field: " + fieldName);
          }

          Universe.errorPrintln();
          break;
        }

        case PUSH_BLOCK: {
          int idx = bytecodes.get(b + 1);
          Universe.errorPrint("block: (index: " + idx + ") ");
          if (m != null) {
            dumpMethod((SMethod) m.getConstant(idx), indent + "\t");
          }
          break;
        }

        case PUSH_CONSTANT: {
          int idx = bytecodes.get(b + 1);

          Universe.errorPrint("(index: " + idx + ")");

          if (m != null) {
            Object constant = m.getConstant(idx);
            SClass constantClass = Types.getClassOf(constant, u);
            Universe.errorPrint(" value: "
                + "(" + constantClass.getName().toString() + ") "
                + constant.toString());
          }
          Universe.errorPrintln();
          break;
        }

        case Q_PUSH_GLOBAL:
        case PUSH_GLOBAL: {
          int idx = bytecodes.get(b + 1);

          Universe.errorPrint("(index: " + idx + ")");
          if (m != null) {
            Universe.errorPrint(" value: "
                + ((SSymbol) m.getConstant(idx)).toString());
          }
          Universe.errorPrintln();
          break;
        }

        case Q_SEND:
        case Q_SEND_1:
        case Q_SEND_2:
        case Q_SEND_3:
        case SEND:
        case SUPER_SEND: {
          int idx = bytecodes.get(b + 1);
          Universe.errorPrint("(index: " + idx + ")");
          if (m != null) {
            Universe.errorPrint(" signature: " + ((SSymbol) m.getConstant(idx)).toString());
          }
          Universe.errorPrintln();
          break;
        }

        case RETURN_NON_LOCAL: {
          Universe.errorPrintln("context: " + bytecodes.get(b + 1));
          break;
        }

        default:
          Universe.errorPrintln("<incorrect bytecode>");
      }
    }
    Universe.errorPrintln(indent + ")");
  }

}
