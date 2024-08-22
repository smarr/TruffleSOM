package trufflesom.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static trufflesom.compiler.bc.Disassembler.dumpMethod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;

import org.junit.Ignore;

import trufflesom.compiler.bc.BytecodeMethodGenContext;
import trufflesom.interpreter.Method;
import trufflesom.interpreter.bc.Bytecodes;
import trufflesom.interpreter.nodes.bc.BytecodeLoopNode;
import trufflesom.vmobjects.SInvokable.SMethod;


@Ignore("provides just setup")
public class BytecodeTestSetup extends TruffleTestSetup {

  protected BytecodeMethodGenContext mgenc;

  protected byte[] getBytecodesOfBlock(final int bytecodeIdx) {
    SMethod blockMethod = (SMethod) mgenc.getConstant(bytecodeIdx);
    Method blockIvkbl = (Method) blockMethod.getInvokable();
    return read(blockIvkbl, "body", BytecodeLoopNode.class).getBytecodeArray();
  }

  protected static class BC {
    final byte   bytecode;
    final Byte   arg1;
    final Byte   arg2;
    final String note;

    BC(final byte bytecode, final int arg1, final int arg2, final String note) {
      this.bytecode = bytecode;
      this.arg1 = (byte) arg1;
      this.arg2 = (byte) arg2;
      this.note = note;
    }

    BC(final byte bytecode, final int arg1) {
      this.bytecode = bytecode;
      this.arg1 = (byte) arg1;
      arg2 = null;
      note = null;
    }

    BC(final byte bytecode, final String note) {
      this.bytecode = bytecode;
      this.arg1 = null;
      arg2 = null;
      this.note = note;
    }

    BC(final byte bytecode, final int arg1, final String note) {
      this.bytecode = bytecode;
      this.arg1 = (byte) arg1;
      arg2 = null;
      this.note = note;
    }

    BC(final byte bytecode, final int arg1, final int arg2) {
      this.bytecode = bytecode;
      this.arg1 = (byte) arg1;
      this.arg2 = (byte) arg2;
      note = null;
    }
  }

  protected Object[] t(final int idx, final Object bc) {
    return new Object[] {idx, bc};
  }

  protected void check(final byte[] actual, final Object... expected) {
    Deque<Object> expectedQ = new ArrayDeque<>(Arrays.asList(expected));

    int i = 0;

    while (i < actual.length && !expectedQ.isEmpty()) {
      byte actualBc = actual[i];

      int bcLength = Bytecodes.getBytecodeLength(actualBc);

      Object expectedBc = expectedQ.peek();
      if (expectedBc instanceof Object[]) {
        Object[] tuple = (Object[]) expectedBc;
        if ((Integer) tuple[0] == i) {
          expectedBc = tuple[1];
        } else {
          if (!(((Integer) tuple[0]) > i)) {
            dump();
          }
          assertTrue(((Integer) tuple[0]) > i);
          i += bcLength;
          continue;
        }
      }

      if (expectedBc instanceof BC) {
        BC bc = (BC) expectedBc;

        assertEquals("Bytecode " + i + " expected " + Bytecodes.getBytecodeName(bc.bytecode)
            + " but got " + Bytecodes.getBytecodeName(actualBc), actualBc, bc.bytecode);

        if (bc.arg1 != null) {
          if (actual[i + 1] != (byte) bc.arg1) {
            dump();
          }
          assertEquals("Bytecode " + i + " expected " + Bytecodes.getBytecodeName(bc.bytecode)
              + "(" + bc.arg1 + ", " + bc.arg2 + ") but got "
              + Bytecodes.getBytecodeName(actualBc) + "(" + actual[i + 1] + ", "
              + actual[i + 2] + ")", (byte) bc.arg1, actual[i + 1]);
        }

        if (bc.arg2 != null) {
          assertEquals((byte) bc.arg2, actual[i + 2]);
        }
      } else {
        if ((byte) expectedBc != actualBc) {
          dump();
        }
        assertEquals(
            "Bytecode " + i + " expected " + Bytecodes.getBytecodeName((byte) expectedBc)
                + " but got " + Bytecodes.getBytecodeName(actualBc),
            (byte) expectedBc, actualBc);
      }

      expectedQ.remove();
      i += bcLength;
    }

    assertTrue(expectedQ.isEmpty());
  }

  public void dump() {
    dumpMethod(mgenc);
  }

  public void dump(final byte[] bytecodes) {
    ArrayList<Byte> bcs = new ArrayList<>(bytecodes.length);
    for (byte b : bytecodes) {
      bcs.add(b);
    }

    dumpMethod(bcs);
  }
}
