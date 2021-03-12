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
package trufflesom.compiler.bc;

import static trufflesom.interpreter.bc.Bytecodes.DEC;
import static trufflesom.interpreter.bc.Bytecodes.DUP;
import static trufflesom.interpreter.bc.Bytecodes.HALT;
import static trufflesom.interpreter.bc.Bytecodes.INC;
import static trufflesom.interpreter.bc.Bytecodes.INC_FIELD;
import static trufflesom.interpreter.bc.Bytecodes.INC_FIELD_PUSH;
import static trufflesom.interpreter.bc.Bytecodes.JUMP;
import static trufflesom.interpreter.bc.Bytecodes.JUMP_ON_FALSE_POP;
import static trufflesom.interpreter.bc.Bytecodes.JUMP_ON_FALSE_TOP_NIL;
import static trufflesom.interpreter.bc.Bytecodes.JUMP_ON_TRUE_POP;
import static trufflesom.interpreter.bc.Bytecodes.JUMP_ON_TRUE_TOP_NIL;
import static trufflesom.interpreter.bc.Bytecodes.POP;
import static trufflesom.interpreter.bc.Bytecodes.POP_ARGUMENT;
import static trufflesom.interpreter.bc.Bytecodes.POP_FIELD;
import static trufflesom.interpreter.bc.Bytecodes.POP_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_ARGUMENT;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_BLOCK;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_CONSTANT;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_FIELD;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_GLOBAL;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.RETURN_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.RETURN_NON_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.RETURN_SELF;
import static trufflesom.interpreter.bc.Bytecodes.SEND;
import static trufflesom.interpreter.bc.Bytecodes.SUPER_SEND;

import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SSymbol;


public final class BytecodeGenerator {
  private BytecodeGenerator() {}

  public static void emitHALT(final BytecodeMethodGenContext mgenc) {
    emit1(mgenc, HALT);
  }

  public static void emitINC(final BytecodeMethodGenContext mgenc) {
    emit1(mgenc, INC);
  }

  public static void emitDEC(final BytecodeMethodGenContext mgenc) {
    emit1(mgenc, DEC);
  }

  public static void emitINCFIELD(final BytecodeMethodGenContext mgenc, final byte fieldIdx,
      final byte ctx) {
    assert fieldIdx >= 0;
    assert ctx >= 0;
    emit3(mgenc, INC_FIELD, fieldIdx, ctx);
  }

  public static void emitINCFIELDPUSH(final BytecodeMethodGenContext mgenc,
      final byte fieldIdx, final byte ctx) {
    assert fieldIdx >= 0;
    assert ctx >= 0;
    emit3(mgenc, INC_FIELD_PUSH, fieldIdx, ctx);
  }

  public static void emitPOP(final BytecodeMethodGenContext mgenc) {
    if (!mgenc.optimizeDupPopPopSequence()) {
      emit1(mgenc, POP);
    }
  }

  public static void emitPUSHARGUMENT(final BytecodeMethodGenContext mgenc, final byte idx,
      final byte ctx) {
    assert idx >= 0;
    assert ctx >= 0;
    emit3(mgenc, PUSH_ARGUMENT, idx, ctx);
  }

  public static void emitRETURNLOCAL(final BytecodeMethodGenContext mgenc) {
    emit1(mgenc, RETURN_LOCAL);
  }

  public static void emitRETURNSELF(final BytecodeMethodGenContext mgenc) {
    emit1(mgenc, RETURN_SELF);
  }

  public static void emitRETURNNONLOCAL(final BytecodeMethodGenContext mgenc) {
    emit2(mgenc, RETURN_NON_LOCAL, mgenc.getMaxContextLevel());
  }

  public static void emitDUP(final BytecodeMethodGenContext mgenc) {
    emit1(mgenc, DUP);
  }

  public static void emitPUSHBLOCK(final BytecodeMethodGenContext mgenc,
      final SMethod blockMethod) {
    byte litIdx = mgenc.findLiteralIndex(blockMethod);
    assert litIdx >= 0;
    emit2(mgenc, PUSH_BLOCK, litIdx);
  }

  public static void emitPUSHLOCAL(final BytecodeMethodGenContext mgenc, final byte idx,
      final byte ctx) {
    assert idx >= 0;
    assert ctx >= 0;
    emit3(mgenc, PUSH_LOCAL, idx, ctx);
  }

  public static void emitPUSHFIELD(final BytecodeMethodGenContext mgenc,
      final SSymbol fieldName) {
    assert mgenc.hasField(fieldName);
    emit3(mgenc, PUSH_FIELD, mgenc.getFieldIndex(fieldName), mgenc.getMaxContextLevel());
  }

  public static void emitPUSHFIELD(final BytecodeMethodGenContext mgenc, final byte fieldIdx,
      final byte ctx) {
    assert fieldIdx >= 0;
    assert ctx >= 0;
    emit3(mgenc, PUSH_FIELD, fieldIdx, ctx);
  }

  public static void emitPUSHGLOBAL(final BytecodeMethodGenContext mgenc,
      final SSymbol global) {
    emit2(mgenc, PUSH_GLOBAL, mgenc.findLiteralIndex(global));
  }

  public static void emitPOPARGUMENT(final BytecodeMethodGenContext mgenc, final byte idx,
      final byte ctx) {
    assert idx >= 0;
    assert ctx >= 0;
    emit3(mgenc, POP_ARGUMENT, idx, ctx);
  }

  public static void emitPOPLOCAL(final BytecodeMethodGenContext mgenc, final byte idx,
      final byte ctx) {
    assert idx >= 0;
    assert ctx >= 0;
    emit3(mgenc, POP_LOCAL, idx, ctx);
  }

  public static void emitPOPFIELD(final BytecodeMethodGenContext mgenc,
      final SSymbol fieldName) {
    assert mgenc.hasField(fieldName);
    emit3(mgenc, POP_FIELD, mgenc.getFieldIndex(fieldName), mgenc.getMaxContextLevel());
  }

  public static void emitPOPFIELD(final BytecodeMethodGenContext mgenc, final byte fieldIdx,
      final byte ctx) {
    assert fieldIdx >= 0;
    assert ctx >= 0;
    emit3(mgenc, POP_FIELD, fieldIdx, ctx);
  }

  public static void emitSUPERSEND(final BytecodeMethodGenContext mgenc, final SSymbol msg) {
    emit2(mgenc, SUPER_SEND, mgenc.findLiteralIndex(msg));
  }

  public static void emitSEND(final BytecodeMethodGenContext mgenc, final SSymbol msg) {
    emit2(mgenc, SEND, mgenc.findLiteralIndex(msg));
  }

  public static void emitPUSHCONSTANT(final BytecodeMethodGenContext mgenc, final Object lit) {
    emit2(mgenc, PUSH_CONSTANT, mgenc.findLiteralIndex(lit));
  }

  public static void emitPUSHCONSTANT(final BytecodeMethodGenContext mgenc,
      final byte literalIndex) {
    assert literalIndex >= 0;
    emit2(mgenc, PUSH_CONSTANT, literalIndex);
  }

  public static void emitJUMP(final BytecodeMethodGenContext mgenc, final byte offset) {
    emit2(mgenc, JUMP, offset);
  }

  public static void emitJUMPONTRUETOPNIL(final BytecodeMethodGenContext mgenc,
      final byte offset) {
    emit2(mgenc, JUMP_ON_TRUE_TOP_NIL, offset);
  }

  public static void emitJUMPONFALSETOPNIL(final BytecodeMethodGenContext mgenc,
      final byte offset) {
    emit2(mgenc, JUMP_ON_FALSE_TOP_NIL, offset);
  }

  public static void emitJUMPONTRUEPOP(final BytecodeMethodGenContext mgenc,
      final byte offset) {
    emit2(mgenc, JUMP_ON_TRUE_POP, offset);
  }

  public static void emitJUMPONFALSEPOP(final BytecodeMethodGenContext mgenc,
      final byte offset) {
    emit2(mgenc, JUMP_ON_FALSE_POP, offset);
  }

  public static int emitJumpOnTrueWithDummyOffset(final BytecodeMethodGenContext mgenc,
      final boolean needsPop) {
    emit1(mgenc, needsPop ? JUMP_ON_TRUE_POP : JUMP_ON_TRUE_TOP_NIL);
    return mgenc.addBytecodeArgumentAndGetIndex((byte) 0);
  }

  public static int emitJumpOnFalseWithDummyOffset(final BytecodeMethodGenContext mgenc,
      final boolean needsPop) {
    emit1(mgenc, needsPop ? JUMP_ON_FALSE_POP : JUMP_ON_FALSE_TOP_NIL);
    return mgenc.addBytecodeArgumentAndGetIndex((byte) 0);
  }

  public static int emitJumpWithDummyOffset(final BytecodeMethodGenContext mgenc) {
    emit1(mgenc, JUMP);
    return mgenc.addBytecodeArgumentAndGetIndex((byte) 0);
  }

  public static void patchJumpOffsetToPointToNextInstruction(
      final BytecodeMethodGenContext mgenc,
      final int idxOfOffset) {
    mgenc.patchJumpOffsetToPointToNextInstruction(idxOfOffset);
  }

  private static void emit1(final BytecodeMethodGenContext mgenc, final byte code) {
    mgenc.addBytecode(code);
  }

  private static void emit2(final BytecodeMethodGenContext mgenc, final byte code,
      final byte idx) {
    mgenc.addBytecode(code);
    mgenc.addBytecodeArgument(idx);
  }

  private static void emit3(final BytecodeMethodGenContext mgenc, final byte code,
      final byte idx, final byte ctx) {
    mgenc.addBytecode(code);
    mgenc.addBytecodeArgument(idx);
    mgenc.addBytecodeArgument(ctx);
  }
}
