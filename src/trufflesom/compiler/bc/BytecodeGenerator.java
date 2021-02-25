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

import static trufflesom.interpreter.bc.Bytecodes.DUP;
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
import static trufflesom.interpreter.bc.Bytecodes.SEND;
import static trufflesom.interpreter.bc.Bytecodes.SUPER_SEND;

import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SSymbol;


public class BytecodeGenerator {

  public void emitPOP(final BytecodeMethodGenContext mgenc) {
    emit1(mgenc, POP);
  }

  public void emitPUSHARGUMENT(final BytecodeMethodGenContext mgenc, final byte idx,
      final byte ctx) {
    emit3(mgenc, PUSH_ARGUMENT, idx, ctx);
  }

  public void emitRETURNLOCAL(final BytecodeMethodGenContext mgenc) {
    emit1(mgenc, RETURN_LOCAL);
  }

  public void emitRETURNNONLOCAL(final BytecodeMethodGenContext mgenc) {
    emit2(mgenc, RETURN_NON_LOCAL, mgenc.getMaxContextLevel());
  }

  public void emitDUP(final BytecodeMethodGenContext mgenc) {
    emit1(mgenc, DUP);
  }

  public void emitPUSHBLOCK(final BytecodeMethodGenContext mgenc, final SMethod blockMethod) {
    emit2(mgenc, PUSH_BLOCK, mgenc.findLiteralIndex(blockMethod));
  }

  public void emitPUSHLOCAL(final BytecodeMethodGenContext mgenc, final byte idx,
      final byte ctx) {
    assert idx >= 0;
    emit3(mgenc, PUSH_LOCAL, idx, ctx);
  }

  public void emitPUSHFIELD(final BytecodeMethodGenContext mgenc, final SSymbol fieldName) {
    assert mgenc.hasField(fieldName);
    emit3(mgenc, PUSH_FIELD, mgenc.getFieldIndex(fieldName), mgenc.getMaxContextLevel());
  }

  public void emitPUSHGLOBAL(final BytecodeMethodGenContext mgenc, final SSymbol global) {
    emit2(mgenc, PUSH_GLOBAL, mgenc.findLiteralIndex(global));
  }

  public void emitPOPARGUMENT(final BytecodeMethodGenContext mgenc, final byte idx,
      final byte ctx) {
    emit3(mgenc, POP_ARGUMENT, idx, ctx);
  }

  public void emitPOPLOCAL(final BytecodeMethodGenContext mgenc, final byte idx,
      final byte ctx) {
    emit3(mgenc, POP_LOCAL, idx, ctx);
  }

  public void emitPOPFIELD(final BytecodeMethodGenContext mgenc, final SSymbol fieldName) {
    assert mgenc.hasField(fieldName);
    emit3(mgenc, POP_FIELD, mgenc.getFieldIndex(fieldName), mgenc.getMaxContextLevel());
  }

  public void emitSUPERSEND(final BytecodeMethodGenContext mgenc, final SSymbol msg) {
    emit2(mgenc, SUPER_SEND, mgenc.findLiteralIndex(msg));
  }

  public void emitSEND(final BytecodeMethodGenContext mgenc, final SSymbol msg) {
    emit2(mgenc, SEND, mgenc.findLiteralIndex(msg));
  }

  public void emitPUSHCONSTANT(final BytecodeMethodGenContext mgenc, final Object lit) {
    emit2(mgenc, PUSH_CONSTANT, mgenc.findLiteralIndex(lit));
  }

  public void emitPUSHCONSTANT(final BytecodeMethodGenContext mgenc, final byte literalIndex) {
    emit2(mgenc, PUSH_CONSTANT, literalIndex);
  }

  private void emit1(final BytecodeMethodGenContext mgenc, final byte code) {
    mgenc.addBytecode(code);
  }

  private void emit2(final BytecodeMethodGenContext mgenc, final byte code, final byte idx) {
    mgenc.addBytecode(code);
    mgenc.addBytecode(idx);
  }

  private void emit3(final BytecodeMethodGenContext mgenc, final byte code, final byte idx,
      final byte ctx) {
    mgenc.addBytecode(code);
    mgenc.addBytecode(idx);
    mgenc.addBytecode(ctx);
  }

}
