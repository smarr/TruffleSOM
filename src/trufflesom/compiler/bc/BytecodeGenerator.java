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
import static trufflesom.interpreter.bc.Bytecodes.JUMP2;
import static trufflesom.interpreter.bc.Bytecodes.JUMP2_BACKWARDS;
import static trufflesom.interpreter.bc.Bytecodes.JUMP_BACKWARDS;
import static trufflesom.interpreter.bc.Bytecodes.JUMP_ON_FALSE_POP;
import static trufflesom.interpreter.bc.Bytecodes.JUMP_ON_FALSE_TOP_NIL;
import static trufflesom.interpreter.bc.Bytecodes.JUMP_ON_TRUE_POP;
import static trufflesom.interpreter.bc.Bytecodes.JUMP_ON_TRUE_TOP_NIL;
import static trufflesom.interpreter.bc.Bytecodes.POP;
import static trufflesom.interpreter.bc.Bytecodes.POP_ARGUMENT;
import static trufflesom.interpreter.bc.Bytecodes.POP_FIELD;
import static trufflesom.interpreter.bc.Bytecodes.POP_FIELD_0;
import static trufflesom.interpreter.bc.Bytecodes.POP_FIELD_1;
import static trufflesom.interpreter.bc.Bytecodes.POP_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.POP_LOCAL_0;
import static trufflesom.interpreter.bc.Bytecodes.POP_LOCAL_1;
import static trufflesom.interpreter.bc.Bytecodes.POP_LOCAL_2;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_0;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_1;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_ARG1;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_ARG2;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_ARGUMENT;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_BLOCK;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_BLOCK_NO_CTX;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_CONSTANT;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_CONSTANT_0;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_CONSTANT_1;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_CONSTANT_2;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_FIELD;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_FIELD_0;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_FIELD_1;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_GLOBAL;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_LOCAL_0;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_LOCAL_1;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_LOCAL_2;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_NIL;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_SELF;
import static trufflesom.interpreter.bc.Bytecodes.RETURN_FIELD_0;
import static trufflesom.interpreter.bc.Bytecodes.RETURN_FIELD_1;
import static trufflesom.interpreter.bc.Bytecodes.RETURN_FIELD_2;
import static trufflesom.interpreter.bc.Bytecodes.RETURN_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.RETURN_NON_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.RETURN_SELF;
import static trufflesom.interpreter.bc.Bytecodes.SEND;
import static trufflesom.interpreter.bc.Bytecodes.SUPER_SEND;
import static trufflesom.vm.SymbolTable.symFalse;
import static trufflesom.vm.SymbolTable.symNil;
import static trufflesom.vm.SymbolTable.symTrue;

import trufflesom.compiler.Parser.ParseError;
import trufflesom.compiler.ParserBc;
import trufflesom.interpreter.nodes.GlobalNode;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SSymbol;


public final class BytecodeGenerator {
  private BytecodeGenerator() {}

  public static void emitHALT(final BytecodeMethodGenContext mgenc) {
    emit1(mgenc, HALT, 0);
  }

  public static void emitINC(final BytecodeMethodGenContext mgenc) {
    emit1(mgenc, INC, 0);
  }

  public static void emitDEC(final BytecodeMethodGenContext mgenc) {
    emit1(mgenc, DEC, 0);
  }

  public static void emitINCFIELD(final BytecodeMethodGenContext mgenc, final byte fieldIdx,
      final byte ctx) {
    assert fieldIdx >= 0;
    assert ctx >= 0;
    emit3(mgenc, INC_FIELD, fieldIdx, ctx, 0);
  }

  public static void emitINCFIELDPUSH(final BytecodeMethodGenContext mgenc,
      final byte fieldIdx, final byte ctx) {
    assert fieldIdx >= 0;
    assert ctx >= 0;
    emit3(mgenc, INC_FIELD_PUSH, fieldIdx, ctx, 1);
  }

  public static void emitPOP(final BytecodeMethodGenContext mgenc) {
    if (!mgenc.optimizeDupPopPopSequence()) {
      emit1(mgenc, POP, -1);
    }
  }

  public static void emitPUSHARGUMENT(final BytecodeMethodGenContext mgenc, final byte idx,
      final byte ctx) {
    assert idx >= 0;
    assert ctx >= 0;
    if (ctx == 0) {
      if (idx == 0) {
        emit1(mgenc, PUSH_SELF, 1);
        return;
      } else if (idx == 1) {
        emit1(mgenc, PUSH_ARG1, 1);
        return;
      } else if (idx == 2) {
        emit1(mgenc, PUSH_ARG2, 1);
        return;
      }
    }
    emit3(mgenc, PUSH_ARGUMENT, idx, ctx, 1);
  }

  public static void emitRETURNLOCAL(final BytecodeMethodGenContext mgenc) {
    if (!mgenc.optimizeReturnField()) {
      emit1(mgenc, RETURN_LOCAL, 0);
    }
  }

  public static void emitRETURNSELF(final BytecodeMethodGenContext mgenc) {
    mgenc.optimizeDupPopPopSequence();
    emit1(mgenc, RETURN_SELF, 0);
  }

  public static void emitRETURNNONLOCAL(final BytecodeMethodGenContext mgenc) {
    emit2(mgenc, RETURN_NON_LOCAL, mgenc.getMaxContextLevel(), 0);
  }

  public static void emitRETURNFIELD(final BytecodeMethodGenContext mgenc, final byte idx) {
    if (idx == 0) {
      emit1(mgenc, RETURN_FIELD_0, 0);
      return;
    } else if (idx == 1) {
      emit1(mgenc, RETURN_FIELD_1, 0);
      return;
    } else if (idx == 2) {
      emit1(mgenc, RETURN_FIELD_2, 0);
      return;
    }
    throw new IllegalArgumentException("RETURN_FIELD bytecode does not support idx=" + idx);
  }

  public static void emitDUP(final BytecodeMethodGenContext mgenc) {
    emit1(mgenc, DUP, 1);
  }

  public static void emitPUSHBLOCK(final BytecodeMethodGenContext mgenc,
      final SMethod blockMethod, final boolean withContext) {
    byte litIdx = mgenc.findLiteralIndex(blockMethod);
    assert litIdx >= 0;
    emit2(mgenc, withContext ? PUSH_BLOCK : PUSH_BLOCK_NO_CTX, litIdx, 1);
  }

  public static void emitPUSHLOCAL(final BytecodeMethodGenContext mgenc, final byte idx,
      final byte ctx) {
    assert idx >= 0;
    assert ctx >= 0;
    if (ctx == 0) {
      if (idx == 0) {
        emit1(mgenc, PUSH_LOCAL_0, 1);
        return;
      } else if (idx == 1) {
        emit1(mgenc, PUSH_LOCAL_1, 1);
        return;
      } else if (idx == 2) {
        emit1(mgenc, PUSH_LOCAL_2, 1);
        return;
      }
    }
    emit3(mgenc, PUSH_LOCAL, idx, ctx, 1);
  }

  public static void emitPUSHFIELD(final BytecodeMethodGenContext mgenc,
      final SSymbol fieldName) {
    byte fieldIdx = mgenc.getFieldIndex(fieldName);
    byte ctx = mgenc.getMaxContextLevel();
    emitPUSHFIELD(mgenc, fieldIdx, ctx);
  }

  public static void emitPUSHFIELD(final BytecodeMethodGenContext mgenc, final byte fieldIdx,
      final byte ctx) {
    assert fieldIdx != -1;
    if (ctx == 0) {
      if (fieldIdx == 0) {
        emit1(mgenc, PUSH_FIELD_0, 1);
        return;
      } else if (fieldIdx == 1) {
        emit1(mgenc, PUSH_FIELD_1, 1);
        return;
      }
    }
    emit3(mgenc, PUSH_FIELD, fieldIdx, ctx, 1);
  }

  public static void emitPUSHGLOBAL(final BytecodeMethodGenContext mgenc,
      final SSymbol global, final ParserBc parser) throws ParseError {
    if (symNil == global) {
      emitPUSHCONSTANT(mgenc, Nil.nilObject, parser);
      return;
    }

    if (symTrue == global) {
      emitPUSHCONSTANT(mgenc, true, parser);
      return;
    }

    if (symFalse == global) {
      emitPUSHCONSTANT(mgenc, false, parser);
      return;
    }

    if (GlobalNode.isPotentiallyUnknown(global, mgenc.getUniverse())) {
      mgenc.markAccessingOuterScopes();
    }
    byte idx = mgenc.addLiteralIfAbsent(global, parser);
    assert idx != -1;
    emit2(mgenc, PUSH_GLOBAL, idx, 1);
  }

  public static void emitPOPARGUMENT(final BytecodeMethodGenContext mgenc, final byte idx,
      final byte ctx) {
    assert idx >= 0;
    assert ctx >= 0;
    emit3(mgenc, POP_ARGUMENT, idx, ctx, -1);
  }

  public static void emitPOPLOCAL(final BytecodeMethodGenContext mgenc, final byte idx,
      final byte ctx) {
    assert idx >= 0;
    assert ctx >= 0;
    if (ctx == 0) {
      if (idx == 0) {
        emit1(mgenc, POP_LOCAL_0, -1);
        return;
      } else if (idx == 1) {
        emit1(mgenc, POP_LOCAL_1, -1);
        return;
      } else if (idx == 2) {
        emit1(mgenc, POP_LOCAL_2, -1);
        return;
      }
    }
    emit3(mgenc, POP_LOCAL, idx, ctx, -1);
  }

  public static void emitPOPFIELD(final BytecodeMethodGenContext mgenc,
      final SSymbol fieldName) {
    byte fieldIdx = mgenc.getFieldIndex(fieldName);
    byte ctx = mgenc.getMaxContextLevel();
    emitPOPFIELD(mgenc, fieldIdx, ctx);
  }

  public static void emitPOPFIELD(final BytecodeMethodGenContext mgenc, final byte fieldIdx,
      final byte ctx) {
    assert fieldIdx != -1;
    if (ctx == 0) {
      if (fieldIdx == 0) {
        emit1(mgenc, POP_FIELD_0, -1);
        return;
      } else if (fieldIdx == 1) {
        emit1(mgenc, POP_FIELD_1, -1);
        return;
      }
    }
    emit3(mgenc, POP_FIELD, fieldIdx, ctx, -1);
  }

  public static void emitSUPERSEND(final BytecodeMethodGenContext mgenc, final SSymbol msg) {
    int stackEffect = -msg.getNumberOfSignatureArguments() + 1; // +1 for the return value
    emit2(mgenc, SUPER_SEND, mgenc.findLiteralIndex(msg), stackEffect);
  }

  public static void emitSEND(final BytecodeMethodGenContext mgenc, final SSymbol msg) {
    int stackEffect = -msg.getNumberOfSignatureArguments() + 1; // +1 for the return value
    emit2(mgenc, SEND, mgenc.findLiteralIndex(msg), stackEffect);
  }

  public static void emitPUSHCONSTANT(final BytecodeMethodGenContext mgenc, final Object lit,
      final ParserBc parser) throws ParseError {
    if (lit instanceof Long) {
      if (0 == (Long) lit) {
        emit1(mgenc, PUSH_0, 1);
        return;
      }

      if (1 == (Long) lit) {
        emit1(mgenc, PUSH_1, 1);
        return;
      }
    }

    if (lit == Nil.nilObject) {
      emit1(mgenc, PUSH_NIL, 1);
      return;
    }

    byte idx = mgenc.addLiteralIfAbsent(lit, parser);
    switch (idx) {
      case 0:
        emit1(mgenc, PUSH_CONSTANT_0, 1);
        break;
      case 1:
        emit1(mgenc, PUSH_CONSTANT_1, 1);
        break;
      case 2:
        emit1(mgenc, PUSH_CONSTANT_2, 1);
        break;
      default:
        emit2(mgenc, PUSH_CONSTANT, idx, 1);
        break;
    }
  }

  public static void emitPUSHCONSTANT(final BytecodeMethodGenContext mgenc,
      final byte literalIndex) {
    assert literalIndex >= 0;
    emit2(mgenc, PUSH_CONSTANT, literalIndex, 1);
  }

  public static int emitJumpOnBoolWithDummyOffset(final BytecodeMethodGenContext mgenc,
      final boolean isIfTrue, final boolean needsPop) {
    // Remember: true and false seem flipped here
    // this is because if the test passes, the block is inlined directly.
    // if the test fails, we need to jump.
    // Thus, an #ifTrue: needs to generated a JUMP_ON_FALSE.
    if (isIfTrue) {
      emit1(mgenc, needsPop ? JUMP_ON_FALSE_POP : JUMP_ON_FALSE_TOP_NIL, needsPop ? -1 : 0);
    } else {
      emit1(mgenc, needsPop ? JUMP_ON_TRUE_POP : JUMP_ON_TRUE_TOP_NIL, needsPop ? -1 : 0);
    }
    int idx = mgenc.addBytecodeArgumentAndGetIndex((byte) 0);
    mgenc.addBytecodeArgument((byte) 0);
    return idx;
  }

  public static void emitJumpWithOffset(final BytecodeMethodGenContext mgenc,
      final byte offset1, final byte offset2) {
    emit3(mgenc, offset2 == 0 ? JUMP : JUMP2, offset1, offset2, 0);
  }

  public static void emitJumpBackwardsWithOffset(final BytecodeMethodGenContext mgenc,
      final byte offset1, final byte offset2) {
    emit3(mgenc, offset2 == 0 ? JUMP_BACKWARDS : JUMP2_BACKWARDS, offset1, offset2, 0);
  }

  public static int emitJumpWithDummyOffset(final BytecodeMethodGenContext mgenc) {
    emit1(mgenc, JUMP, 0);
    int idx = mgenc.addBytecodeArgumentAndGetIndex((byte) 0);
    mgenc.addBytecodeArgument((byte) 0);
    return idx;
  }

  public static int emit3WithDummy(final BytecodeMethodGenContext mgenc, final byte code,
      final int stackEffect) {
    mgenc.addBytecode(code, stackEffect);
    int idx = mgenc.addBytecodeArgumentAndGetIndex((byte) 0);
    mgenc.addBytecodeArgument((byte) 0);
    return idx;
  }

  public static void patchJumpOffsetToPointToNextInstruction(
      final BytecodeMethodGenContext mgenc,
      final int idxOfOffset, final ParserBc parser) throws ParseError {
    mgenc.patchJumpOffsetToPointToNextInstruction(idxOfOffset, parser);
  }

  public static void emit1(final BytecodeMethodGenContext mgenc, final byte code,
      final int stackEffect) {
    mgenc.addBytecode(code, stackEffect);
  }

  public static void emit2(final BytecodeMethodGenContext mgenc, final byte code,
      final byte idx, final int stackEffect) {
    mgenc.addBytecode(code, stackEffect);
    mgenc.addBytecodeArgument(idx);
  }

  public static void emit3(final BytecodeMethodGenContext mgenc, final byte code,
      final byte idx, final byte ctx, final int stackEffect) {
    mgenc.addBytecode(code, stackEffect);
    mgenc.addBytecodeArgument(idx);
    mgenc.addBytecodeArgument(ctx);
  }
}
