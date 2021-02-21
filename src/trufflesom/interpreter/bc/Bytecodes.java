/**
 * Copyright (c) 2017 Michael Haupt, github@haupz.de
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
package trufflesom.interpreter.bc;

import java.util.stream.Stream;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;


public class Bytecodes {

  // Bytecodes used by the simple object machine
  public static final byte HALT             = 0;
  public static final byte DUP              = 1;
  public static final byte PUSH_LOCAL       = 2;
  public static final byte PUSH_ARGUMENT    = 3;
  public static final byte PUSH_FIELD       = 4;
  public static final byte PUSH_BLOCK       = 5;
  public static final byte PUSH_CONSTANT    = 6;
  public static final byte PUSH_GLOBAL      = 7;
  public static final byte POP              = 8;
  public static final byte POP_LOCAL        = 9;
  public static final byte POP_ARGUMENT     = 10;
  public static final byte POP_FIELD        = 11;
  public static final byte SEND             = 12;
  public static final byte SUPER_SEND       = 13;
  public static final byte RETURN_LOCAL     = 14;
  public static final byte RETURN_NON_LOCAL = 15;

  private static final String[] PADDED_BYTECODE_NAMES = new String[] {
      "HALT            ", "DUP             ", "PUSH_LOCAL      ",
      "PUSH_ARGUMENT   ", "PUSH_FIELD      ", "PUSH_BLOCK      ",
      "PUSH_CONSTANT   ", "PUSH_GLOBAL     ", "POP             ",
      "POP_LOCAL       ", "POP_ARGUMENT    ", "POP_FIELD       ",
      "SEND            ", "SUPER_SEND      ", "RETURN_LOCAL    ",
      "RETURN_NON_LOCAL"
  };

  private static final String[] BYTECODE_NAMES =
      Stream.of(PADDED_BYTECODE_NAMES).map(String::trim).toArray(String[]::new);

  private static final byte NUM_BYTECODES = (byte) BYTECODE_NAMES.length;

  private static void checkBytecodeIndex(final byte bytecode) {
    if (bytecode < 0 || bytecode >= NUM_BYTECODES) {
      throw new IllegalArgumentException("illegal bytecode: " + bytecode);
    }
  }

  public static String getBytecodeName(final byte bytecode) {
    checkBytecodeIndex(bytecode);
    return BYTECODE_NAMES[bytecode];
  }

  public static String getPaddedBytecodeName(final byte bytecode) {
    checkBytecodeIndex(bytecode);
    return PADDED_BYTECODE_NAMES[bytecode];
  }

  public static int getBytecodeLength(final byte bytecode) {
    // Return the length of the given bytecode
    return BYTECODE_LENGTH[bytecode];
  }

  // Static array holding lengths of each bytecode
  @CompilationFinal(dimensions = 1) private static final int[] BYTECODE_LENGTH = new int[] {
      1, // HALT
      1, // DUP
      3, // PUSH_LOCAL
      3, // PUSH_ARGUMENT
      2, // PUSH_FIELD
      2, // PUSH_BLOCK
      2, // PUSH_CONSTANT
      2, // PUSH_GLOBAL
      1, // POP
      3, // POP_LOCAL
      3, // POP_ARGUMENT
      2, // POP_FIELD
      2, // SEND
      2, // SUPER_SEND
      1, // RETURN_LOCAL
      1 // RETURN_NON_LOCAL
  };
}
