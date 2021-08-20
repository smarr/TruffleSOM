package trufflesom.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static trufflesom.vm.SymbolTable.symbolFor;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

import org.junit.Ignore;
import org.junit.Test;

import com.oracle.truffle.api.source.Source;

import bd.basic.ProgramDefinitionError;
import trufflesom.compiler.ParserBc;
import trufflesom.interpreter.SomLanguage;
import trufflesom.interpreter.bc.Bytecodes;
import trufflesom.interpreter.nodes.bc.BytecodeLoopNode;
import trufflesom.vmobjects.SInvokable.SMethod;


public class BytecodeMethodTests extends BytecodeTestSetup {

  private byte[] methodToBytecodes(final String source) {
    Source s = SomLanguage.getSyntheticSource(source, "test");

    ParserBc parser = new ParserBc(source, s, probe, universe);
    try {
      parser.method(mgenc);
    } catch (ProgramDefinitionError e) {
      throw new RuntimeException(e);
    }
    return mgenc.getBytecodeArray();
  }

  private static class BC {
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

  private Object[] t(final int idx, final Object bc) {
    return new Object[] {idx, bc};
  }

  private void check(final byte[] actual, final Object... expected) {
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
          assertEquals("Bytecode " + i + " expected " + Bytecodes.getBytecodeName(bc.bytecode)
              + "(" + bc.arg1 + ", " + bc.arg2 + ") but got "
              + Bytecodes.getBytecodeName(actualBc) + "(" + actual[i + 1] + ", "
              + actual[i + 2] + ")", actual[i + 1], (byte) bc.arg1);
        }

        if (bc.arg2 != null) {
          assertEquals(actual[i + 2], (byte) bc.arg2);
        }
      } else {
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

  private void incDecBytecodes(final String op, final byte bytecode) {
    initMgenc();
    byte[] bytecodes = methodToBytecodes("test = ( 1 " + op + "  1 )");

    assertEquals(4, bytecodes.length);
    check(bytecodes, Bytecodes.PUSH_CONSTANT, bytecode, Bytecodes.RETURN_SELF);

    fail(
        "TODO: first BC should really be a PUSH_1 bytecode, but the optimization is not yet implemented");
  }

  @Ignore("TODO")
  @Test
  public void testIncDecBytecodes() {
    incDecBytecodes("+", Bytecodes.INC);
    incDecBytecodes("-", Bytecodes.DEC);
  }

  @Test
  public void testEmptyMethodReturnsSelf() {
    byte[] bytecodes = methodToBytecodes("test = ( )");

    assertEquals(1, bytecodes.length);
    check(bytecodes, Bytecodes.RETURN_SELF);
  }

  @Test
  public void testExplicitReturnSelf() {
    byte[] bytecodes = methodToBytecodes("test = ( ^ self )");

    assertEquals(1, bytecodes.length);
    check(bytecodes, Bytecodes.RETURN_SELF);
  }

  @Ignore("TODO")
  @Test
  public void testDupPopArgumentPop() {
    byte[] bytecodes = methodToBytecodes("test: arg = ( arg := 1. ^ self )");

    assertEquals(6, bytecodes.length);
    check(bytecodes, Bytecodes.PUSH_CONSTANT, Bytecodes.POP_ARGUMENT, Bytecodes.RETURN_SELF);

    fail(
        "TODO: first BC should really be a PUSH_1 bytecode, but the optimization is not yet implemented");
  }

  @Ignore("TODO")
  @Test
  public void testDupPopArgumentPopImplicitReturnSelf() {
    byte[] bytecodes = methodToBytecodes("test: arg = ( arg := 1 )");

    assertEquals(6, bytecodes.length);
    check(bytecodes, Bytecodes.PUSH_CONSTANT, Bytecodes.POP_ARGUMENT, Bytecodes.RETURN_SELF);

    fail(
        "TODO: first BC should really be a PUSH_1 bytecode, but the optimization is not yet implemented");
  }

  @Ignore("TODO")
  @Test
  public void testDupPopLocalPop() {
    byte[] bytecodes = methodToBytecodes("test = ( | local | local := 1. ^ self )");

    assertEquals(6, bytecodes.length);
    check(bytecodes, Bytecodes.PUSH_CONSTANT, Bytecodes.POP_LOCAL, Bytecodes.RETURN_SELF);

    fail(
        "TODO: first BC should really be a PUSH_1 bytecode, but the optimization is not yet implemented");
  }

  @Ignore("TODO")
  @Test
  public void testDupPopField0Pop() {
    addField("field");
    byte[] bytecodes = methodToBytecodes("test = ( field := 1. ^ self )");

    assertEquals(6, bytecodes.length);
    check(bytecodes, Bytecodes.PUSH_CONSTANT, Bytecodes.POP_FIELD, Bytecodes.RETURN_SELF);

    fail("TODO: should be PUSH_1, POP_FIELD_0");
  }

  @Ignore("TODO")
  @Test
  public void testDupPopFieldNPop() {
    addField("a");
    addField("b");
    addField("c");
    addField("d");
    addField("e");
    addField("field");
    byte[] bytecodes = methodToBytecodes("test = ( field := 1. ^ self )");

    assertEquals(6, bytecodes.length);
    check(bytecodes, Bytecodes.PUSH_CONSTANT, Bytecodes.POP_FIELD, Bytecodes.RETURN_SELF);
    fail("TODO: should be PUSH_1,...");
  }

  @Ignore("TODO")
  @Test
  public void testDupPopFieldReturnSelf() {
    addField("field");
    byte[] bytecodes = methodToBytecodes("test: val = ( field := val )");

    assertEquals(7, bytecodes.length);
    check(bytecodes, Bytecodes.PUSH_ARGUMENT, Bytecodes.POP_FIELD, Bytecodes.RETURN_SELF);
    fail("TODO: should be ...,POP_FIELD_0,...");
  }

  @Test
  public void testDupPopFieldNReturnSelf() {
    addField("a");
    addField("b");
    addField("c");
    addField("d");
    addField("e");
    addField("field");
    byte[] bytecodes = methodToBytecodes("test: val = ( field := val )");

    assertEquals(7, bytecodes.length);
    check(bytecodes, Bytecodes.PUSH_ARGUMENT, Bytecodes.POP_FIELD, Bytecodes.RETURN_SELF);
  }

  @Ignore("TODO")
  @Test
  public void testSendDupPopFieldReturnLocal() {
    addField("field");
    byte[] bytecodes = methodToBytecodes("test = ( ^ field := self method )");

    assertEquals(10, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_ARGUMENT,
        Bytecodes.SEND,
        Bytecodes.DUP,
        Bytecodes.POP_FIELD,
        Bytecodes.RETURN_LOCAL);

    fail("TODO: should be ...,POP_FIELD_0,...");
  }

  @Ignore("TODO")
  @Test
  public void testSendDupPopFieldReturnLocalPeriod() {
    addField("field");
    byte[] bytecodes = methodToBytecodes("test = ( ^ field := self method. )");

    assertEquals(10, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_ARGUMENT,
        Bytecodes.SEND,
        Bytecodes.DUP,
        Bytecodes.POP_FIELD,
        Bytecodes.RETURN_LOCAL);

    fail("TODO: should be ...,POP_FIELD_0,...");
  }

  private void ifTrueWithLiteralReturn(final String literal, final byte bytecode) {
    initMgenc();
    byte[] bytecodes = methodToBytecodes("test = (\n"
        + "  self method ifTrue: [ " + literal + " ].\n"
        + ")");

    int bcLength = Bytecodes.getBytecodeLength(bytecode);

    assertEquals(10 + bcLength, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_ARGUMENT,
        Bytecodes.SEND,
        Bytecodes.JUMP_ON_FALSE_TOP_NIL,
        bytecode,
        Bytecodes.POP,
        Bytecodes.RETURN_SELF);

  }

  @Ignore("TODO")
  @Test
  public void testIfTrueWithLiteralReturn() {
    ifTrueWithLiteralReturn("0", Bytecodes.PUSH_CONSTANT);
    ifTrueWithLiteralReturn("1", Bytecodes.PUSH_CONSTANT);
    ifTrueWithLiteralReturn("-10", Bytecodes.PUSH_CONSTANT);
    ifTrueWithLiteralReturn("3333", Bytecodes.PUSH_CONSTANT);
    ifTrueWithLiteralReturn("'str'", Bytecodes.PUSH_CONSTANT);
    ifTrueWithLiteralReturn("#sym", Bytecodes.PUSH_CONSTANT);
    ifTrueWithLiteralReturn("1.1", Bytecodes.PUSH_CONSTANT);
    ifTrueWithLiteralReturn("-2342.234", Bytecodes.PUSH_CONSTANT);
    ifTrueWithLiteralReturn("true", Bytecodes.PUSH_CONSTANT);
    ifTrueWithLiteralReturn("false", Bytecodes.PUSH_CONSTANT);
    ifTrueWithLiteralReturn("nil", Bytecodes.PUSH_CONSTANT);
    ifTrueWithLiteralReturn("SomeGlobal", Bytecodes.PUSH_GLOBAL);
    ifTrueWithLiteralReturn("[]", Bytecodes.PUSH_BLOCK_NO_CTX);
    ifTrueWithLiteralReturn("[ self ]", Bytecodes.PUSH_BLOCK);

    fail("TODO: support for push_0, push_1, ...");
  }

  private void ifArg(final String selector, final byte jumpBytecode) {
    initMgenc();
    byte[] bytecodes = methodToBytecodes(
        "test: arg = (\n"
            + "  #start.\n"
            + "  self method " + selector + " [ arg ].\n"
            + "  #end\n"
            + ")");

    assertEquals(18, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_CONSTANT,
        Bytecodes.POP,
        Bytecodes.PUSH_ARGUMENT,
        Bytecodes.SEND,
        new BC(jumpBytecode, 6),
        new BC(Bytecodes.PUSH_ARGUMENT, 1, 0),
        Bytecodes.POP,
        Bytecodes.PUSH_CONSTANT,
        Bytecodes.RETURN_SELF);

    fail("TODO: push_constant_0, ...");
  }

  @Ignore("TODO")
  @Test
  public void testIfArg() {
    ifArg("ifTrue:", Bytecodes.JUMP_ON_FALSE_TOP_NIL);
    ifArg("ifFalse:", Bytecodes.JUMP_ON_TRUE_TOP_NIL);
  }

  @Test
  public void testKeywordIfTrueArg() {
    byte[] bytecodes = methodToBytecodes(
        "test: arg = (\n"
            + "  #start.\n"
            + "  (self key: 5) ifTrue: [ arg ].\n"
            + "  #end\n"
            + ")");

    assertEquals(20, bytecodes.length);
    check(bytecodes,
        t(8, Bytecodes.SEND),
        new BC(Bytecodes.JUMP_ON_FALSE_TOP_NIL, 6),
        new BC(Bytecodes.PUSH_ARGUMENT, 1, 0),
        Bytecodes.POP,
        Bytecodes.PUSH_CONSTANT);

  }

  @Test
  public void testIfTrueAndIncField() {
    addField("field");
    byte[] bytecodes = methodToBytecodes(
        "test: arg = (\n"
            + "  #start.\n"
            + "  (self key: 5) ifTrue: [ field := field + 1 ].\n"
            + "  #end\n"
            + ")");

    assertEquals(25, bytecodes.length);
    dump();
    check(bytecodes,
        t(8, Bytecodes.SEND),
        new BC(Bytecodes.JUMP_ON_FALSE_TOP_NIL, 11), // jump to pop bytecode, which is the dot
        new BC(Bytecodes.PUSH_FIELD, 0, 0),
        Bytecodes.INC,
        Bytecodes.DUP,
        new BC(Bytecodes.POP_FIELD, 0, 0),
        Bytecodes.POP,
        Bytecodes.PUSH_CONSTANT);
  }

  @Ignore("TODO")
  @Test
  public void testIfTrueAndIncArg() {
    addField("field");
    byte[] bytecodes = methodToBytecodes(
        "test: arg = (\n"
            + "  #start.\n"
            + "  (self key: 5) ifTrue: [ arg + 1 ].\n"
            + "  #end\n"
            + ")");

    assertEquals(25, bytecodes.length);
    dump();
    check(bytecodes,
        t(8, Bytecodes.SEND),
        new BC(Bytecodes.JUMP_ON_FALSE_TOP_NIL, 7), // jump to pop bytecode, which is the dot
        new BC(Bytecodes.PUSH_ARGUMENT, 1, 0),
        Bytecodes.INC,
        Bytecodes.POP,
        Bytecodes.PUSH_CONSTANT);
  }

  private void ifReturnNonLocal(final String ifSelector, final byte jumpBytecode) {
    initMgenc();
    byte[] bytecodes = methodToBytecodes(
        "test: arg = (\n"
            + "  #start.\n"
            + "  (self key: 5) " + ifSelector + " [ ^ arg ].\n"
            + "  #end\n"
            + ")");

    assertEquals(22, bytecodes.length);
    dump();

    fail("TODO: change things so that we can eliminate the HALT bytecode, as in PySOM");
    check(bytecodes,
        t(8, Bytecodes.SEND),
        new BC(jumpBytecode, 8), // jump to pop bytecode, which is the dot
        new BC(Bytecodes.PUSH_ARGUMENT, 1, 0),
        Bytecodes.RETURN_LOCAL,
        Bytecodes.POP);
  }

  @Ignore("TODO")
  @Test
  public void testIfReturnNonLocal() {
    ifReturnNonLocal("ifTrue:", Bytecodes.JUMP_ON_FALSE_TOP_NIL);
    ifReturnNonLocal("ifFalse:", Bytecodes.JUMP_ON_TRUE_TOP_NIL);
  }

  @Ignore("TODO")
  @Test
  public void testNestedIfs() {
    addField("field");
    byte[] bytecodes = methodToBytecodes(
        "test: arg = (\n"
            + "  true ifTrue: [\n"
            + "    false ifFalse: [\n"
            + "      ^ field - arg\n"
            + "    ]\n"
            + "  ]\n"
            + ")");

    fail("TODO: change things so that we can eliminate the HALT bytecode, as in PySOM");
    check(bytecodes,
        Bytecodes.PUSH_GLOBAL,
        new BC(Bytecodes.JUMP_ON_FALSE_TOP_NIL, 18),
        t(7, Bytecodes.JUMP_ON_TRUE_TOP_NIL),
        new BC(Bytecodes.PUSH_FIELD, 0, 0),
        new BC(Bytecodes.PUSH_ARGUMENT, 1, 0),
        Bytecodes.SEND,
        Bytecodes.RETURN_LOCAL,
        Bytecodes.RETURN_SELF);
  }

  @Ignore("TODO")
  @Test
  public void testNestedIfsAndLocals() {
    addField("field");
    byte[] bytecodes = methodToBytecodes(
        "test: arg = (\n"
            + "  | a b c d |\n"
            + "  a := b.\n"
            + "  true ifTrue: [\n"
            + "    | e f g |\n"
            + "    e := 2.\n"
            + "    c := 3.\n"
            + "    false ifFalse: [\n"
            + "      | h i j |\n"
            + "      h := 1.\n"
            + "      ^ i - j - f - g - d ] ] )");

    assertEquals(55, bytecodes.length);
    check(bytecodes,
        new BC(Bytecodes.PUSH_LOCAL, 1, 0),
        new BC(Bytecodes.POP_LOCAL, 0, 0),
        t(8, new BC(Bytecodes.JUMP_ON_FALSE_TOP_NIL, 46)),
        t(13, new BC(Bytecodes.POP_LOCAL, 4, 0)),
        t(18, new BC(Bytecodes.POP_LOCAL, 2, 0)),
        t(23, new BC(Bytecodes.JUMP_ON_TRUE_TOP_NIL, 31)),
        t(27, new BC(Bytecodes.POP_LOCAL, 7, 0)),
        t(48, new BC(Bytecodes.PUSH_LOCAL, 3, 0)));
  }

  @Ignore("TODO")
  @Test
  public void testNestedIfsAndNonInlinedBlocks() {
    addField("field");
    byte[] bytecodes = methodToBytecodes(
        "test: arg = (\n"
            + " | a |\n"
            + " a := 1.\n"
            + " true ifTrue: [\n"
            + "   | e |\n"
            + "   e := 0.\n"
            + "   [ a := 1. a ].\n"
            + "   false ifFalse: [\n"
            + "     | h |\n"
            + "     h := 1.\n"
            + "     [ h + a + e ].\n"
            + "     ^ h ] ].\n"
            + " [ a ]\n"
            + ")");

    assertEquals(36, bytecodes.length);
    check(bytecodes,
        t(4, Bytecodes.PUSH_GLOBAL),
        new BC(Bytecodes.JUMP_ON_FALSE_TOP_NIL, 26),
        t(10, new BC(Bytecodes.POP_LOCAL, 1, 0, "local e")),
        t(18, new BC(Bytecodes.JUMP_ON_TRUE_TOP_NIL, 14)),
        t(22, new BC(Bytecodes.POP_LOCAL, 2, 0, "local h")));

    check(getBytecodesOfBlock(13),
        t(1, new BC(Bytecodes.POP_LOCAL, 0, 1, "local a")));

    check(getBytecodesOfBlock(25),
        new BC(Bytecodes.PUSH_LOCAL, 2, 1, "local h"),
        new BC(Bytecodes.PUSH_LOCAL, 0, 1, "local a"),
        t(8, new BC(Bytecodes.PUSH_LOCAL, 1, 1, "local e")));

    check(getBytecodesOfBlock(33),
        new BC(Bytecodes.PUSH_LOCAL, 0, 1, "local a"));

  }

  @Ignore("TODO")
  @Test
  public void testNestedNonInlinedBlocks() {
    addField("field");
    byte[] bytecodes = methodToBytecodes(
        "test: a = ( | b |\n"
            + " true ifFalse: [ | c |\n"
            + "   a. b. c.\n"
            + "   [:d |\n"
            + "     a. b. c. d.\n"
            + "     [:e |\n"
            + "       a. b. c. d. e ] ] ]\n"
            + ")");

    assertEquals(20, bytecodes.length);
    check(bytecodes,
        t(2, new BC(Bytecodes.JUMP_ON_TRUE_TOP_NIL, 17)),
        new BC(Bytecodes.PUSH_ARGUMENT, 1, 0, "arg a"),
        t(9, new BC(Bytecodes.PUSH_LOCAL, 0, 0, "local b")),
        t(13, new BC(Bytecodes.PUSH_LOCAL, 1, 0, "local c")));

    SMethod blockMethod = (SMethod) mgenc.getConstant(17);
    BytecodeLoopNode blockNode =
        read(blockMethod.getInvokable(), "expressionOrSequence", BytecodeLoopNode.class);
    byte[] blockBytecodes = blockNode.getBytecodeArray();

    check(blockBytecodes,
        new BC(Bytecodes.PUSH_ARGUMENT, 1, 1, "arg a"),
        t(4, new BC(Bytecodes.PUSH_LOCAL, 0, 1, "local b")),
        t(8, new BC(Bytecodes.PUSH_LOCAL, 1, 1, "local c")),
        t(12, new BC(Bytecodes.PUSH_ARGUMENT, 1, 0, "arg d")));

    blockMethod = (SMethod) blockNode.getConstant(16);

    check(
        read(blockMethod.getInvokable(), "expressionOrSequence",
            BytecodeLoopNode.class).getBytecodeArray(),
        new BC(Bytecodes.PUSH_ARGUMENT, 1, 2, "arg a"),
        t(4, new BC(Bytecodes.PUSH_LOCAL, 0, 2, "local b")),
        t(8, new BC(Bytecodes.PUSH_LOCAL, 1, 2, "local c")),
        t(12, new BC(Bytecodes.PUSH_ARGUMENT, 1, 1, "arg d")),
        t(16, new BC(Bytecodes.PUSH_ARGUMENT, 1, 0, "arg e")));
  }

  @Ignore("TODO")
  @Test
  public void testIfTrueIfFalseArg() {
    byte[] bytecodes = methodToBytecodes(
        "test: arg1 with: arg2 = (\n"
            + " #start.\n"
            + " self method ifTrue: [ arg1 ] ifFalse: [ arg2 ].\n"
            + " #end\n"
            + ")");

    assertEquals(23, bytecodes.length);
    check(bytecodes,
        t(7, new BC(Bytecodes.JUMP_ON_FALSE_POP, 9)),
        new BC(Bytecodes.PUSH_ARGUMENT, 1, 0),
        new BC(Bytecodes.JUMP, 6),
        new BC(Bytecodes.PUSH_ARGUMENT, 2, 0),
        Bytecodes.POP);
  }

  @Ignore("TODO")
  @Test
  public void testIfTrueIfFalseNlrArg1() {
    byte[] bytecodes = methodToBytecodes(
        "test: arg1 with: arg2 = (\n"
            + "  #start.\n"
            + "  self method ifTrue: [ ^ arg1 ] ifFalse: [ arg2 ].\n"
            + "  #end\n"
            + ")");

    assertEquals(24, bytecodes.length);
    check(bytecodes,
        t(7, new BC(Bytecodes.JUMP_ON_FALSE_POP, 10)),
        new BC(Bytecodes.PUSH_ARGUMENT, 1, 0),
        Bytecodes.RETURN_LOCAL,
        new BC(Bytecodes.JUMP, 6),
        new BC(Bytecodes.PUSH_ARGUMENT, 2, 0),
        Bytecodes.POP);
  }

  @Ignore("TODO")
  @Test
  public void testIfTrueIfFalseNlrArg2() {
    byte[] bytecodes = methodToBytecodes(
        "test: arg1 with: arg2 = (\n"
            + "  #start.\n"
            + "  self method ifTrue: [ arg1 ] ifFalse: [ ^ arg2 ].\n"
            + "  #end\n"
            + ")");

    assertEquals(24, bytecodes.length);
    check(bytecodes,
        t(7, new BC(Bytecodes.JUMP_ON_FALSE_POP, 9)),
        new BC(Bytecodes.PUSH_ARGUMENT, 1, 0),
        new BC(Bytecodes.JUMP, 7),
        new BC(Bytecodes.PUSH_ARGUMENT, 2, 0),
        Bytecodes.RETURN_LOCAL,
        Bytecodes.POP);
  }

  private void ifTrueIfFalseReturn(final String sel1, final String sel2,
      final byte jumpBytecode) {
    byte[] bytecodes = methodToBytecodes(
        "test: arg1 with: arg2 = (\n"
            + "  #start.\n"
            + "  ^ self method " + sel1 + " [ ^ arg1 ] " + sel2 + " [ arg2 ]\n"
            + ")");

    assertEquals(21, bytecodes.length);
    check(bytecodes,
        t(7, new BC(jumpBytecode, 10)),
        t(17, new BC(Bytecodes.JUMP, 6)));
  }

  @Ignore("TODO")
  @Test
  public void testIfTrueIfFalseReturn() {
    ifTrueIfFalseReturn("ifTrue:", "ifFalse:", Bytecodes.JUMP_ON_FALSE_POP);
    ifTrueIfFalseReturn("ifFalse:", "ifTrue:", Bytecodes.JUMP_ON_TRUE_POP);
  }

  @Ignore("TODO")
  @Test
  public void testIfPushConsantSame() {
    byte[] bytecodes = methodToBytecodes(
        "test = (\n"
            + "  #a. #b. #c. #d.\n"
            + "  true ifFalse: [ #a. #b. #c. #d. ]\n"
            + ")");

    assertEquals(23, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_CONSTANT,
        t(2, Bytecodes.PUSH_CONSTANT),
        t(4, Bytecodes.PUSH_CONSTANT),
        t(6, Bytecodes.PUSH_CONSTANT),
        t(11, new BC(Bytecodes.JUMP_ON_TRUE_TOP_NIL, 11)),
        t(14, Bytecodes.PUSH_CONSTANT),
        t(16, Bytecodes.PUSH_CONSTANT),
        t(18, Bytecodes.PUSH_CONSTANT),
        t(20, new BC(Bytecodes.PUSH_CONSTANT, 3)));
  }

  @Ignore("TODO")
  @Test
  public void testIfPushConsantDifferent() {
    byte[] bytecodes = methodToBytecodes(
        "test = (\n"
            + "  #a. #b. #c. #d.\n"
            + "  true ifFalse: [ #e. #f. #g. #h. ]\n"
            + ")");

    assertEquals(26, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_CONSTANT,
        t(2, Bytecodes.PUSH_CONSTANT),
        t(4, Bytecodes.PUSH_CONSTANT),
        t(6, Bytecodes.PUSH_CONSTANT),
        t(11, new BC(Bytecodes.JUMP_ON_TRUE_TOP_NIL, 14)),
        t(14, new BC(Bytecodes.PUSH_CONSTANT, 6)),
        t(17, new BC(Bytecodes.PUSH_CONSTANT, 7)),
        t(20, new BC(Bytecodes.PUSH_CONSTANT, 8)),
        t(23, new BC(Bytecodes.PUSH_CONSTANT, 9)));
  }

  @Ignore("TODO")
  @Test
  public void testIfInlineAndConstantBcLength() {
    byte[] bytecodes = methodToBytecodes(
        " test = (\n"
            + "  #a. #b. #c.\n"
            + "  true ifTrue: [\n"
            + "    true ifFalse: [ #e. #f. #g ] ]\n"
            + ")");

    assertEquals(Bytecodes.JUMP_ON_TRUE_TOP_NIL, bytecodes[13]);
    assertEquals("jump offset, should point to correct bytecode"
        + " and not be affected by changing length of bytecodes in the block",
        11, bytecodes[14]);
  }

  private void whileInlining(final String sel, final byte jumpBytecode) {
    byte[] bytecodes = methodToBytecodes(
        "test: arg = (\n"
            + "  #start.\n"
            + "  [ true ] " + sel + " [ arg ].\n"
            + "  #end\n"
            + ")");

    assertEquals(19, bytecodes.length);
    check(bytecodes,
        t(2, Bytecodes.PUSH_CONSTANT),
        jumpBytecode,
        Bytecodes.PUSH_ARGUMENT,
        Bytecodes.POP,
        new BC(Bytecodes.JUMP_BACKWARDS, 9),
        Bytecodes.PUSH_CONSTANT, // TODO: push_nil
        Bytecodes.POP);
  }

  @Ignore("TODO")
  @Test
  public void testWhileInlining() {
    whileInlining("whileTrue:", Bytecodes.JUMP_ON_FALSE_POP);
    whileInlining("whileFalse:", Bytecodes.JUMP_ON_TRUE_POP);
  }

  @Ignore("TODO")
  @Test
  public void testInliningWhileLoopWithExpandingBranches() {
    byte[] bytecodes = methodToBytecodes(
        "test = (\n"
            + "  #const0. #const1. #const2.\n"
            + "  0 ifTrue: [\n"
            + "    [ #const3. #const4. #const5 ]\n"
            + "       whileTrue: [\n"
            + "         #const6. #const7. #const8 ]\n"
            + "    ].\n"
            + "    ^ #end\n"
            + ")");

    assertEquals(38, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_CONSTANT,
        Bytecodes.POP,
        Bytecodes.PUSH_CONSTANT,
        Bytecodes.POP,
        Bytecodes.PUSH_CONSTANT,
        Bytecodes.POP,
        Bytecodes.PUSH_CONSTANT,
        new BC(Bytecodes.JUMP_ON_FALSE_TOP_NIL, 27,
            "jump to the pop BC after the if/right before the push #end"),
        Bytecodes.PUSH_CONSTANT,
        Bytecodes.POP,
        Bytecodes.PUSH_CONSTANT,
        Bytecodes.POP,
        Bytecodes.PUSH_CONSTANT,
        new BC(Bytecodes.JUMP_ON_FALSE_POP, 15,
            "jump to push_nil as result of whileTrue"),
        Bytecodes.PUSH_CONSTANT,
        Bytecodes.POP,
        Bytecodes.PUSH_CONSTANT,
        Bytecodes.POP,
        Bytecodes.PUSH_CONSTANT,
        Bytecodes.POP,
        new BC(Bytecodes.JUMP_BACKWARDS, 20,
            "jump to push_nil as result of whileTrue"),

        Bytecodes.PUSH_ARGUMENT,
        Bytecodes.POP,
        new BC(Bytecodes.JUMP_BACKWARDS, 9,
            "jump back to the first push constant in the condition, pushing const3"),
        Bytecodes.PUSH_CONSTANT, // TODO: push_nil
        Bytecodes.POP);
  }

  @Ignore("TODO")
  @Test
  public void testInliningWhileLoopWithContractingBranches() {
    byte[] bytecodes = methodToBytecodes(
        "test = (\n"
            + " 0 ifTrue: [\n"
            + "   [ ^ 1 ]\n"
            + "     whileTrue: [\n"
            + "         ^ 0 ]\n"
            + " ].\n"
            + " ^ #end\n"
            + ")");

    assertEquals(19, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_CONSTANT,
        new BC(Bytecodes.JUMP_ON_FALSE_TOP_NIL, 15,
            "jump to the pop after the if, before pushing #end"),

        Bytecodes.PUSH_CONSTANT,
        Bytecodes.RETURN_LOCAL,

        new BC(Bytecodes.JUMP_ON_FALSE_POP, 9,
            "jump to push_nil as result of whileTrue"),

        Bytecodes.PUSH_CONSTANT,
        Bytecodes.RETURN_LOCAL,
        Bytecodes.POP,

        new BC(Bytecodes.JUMP_BACKWARDS, 8,
            "jump to the push_1 of the condition"),
        Bytecodes.PUSH_CONSTANT,
        Bytecodes.POP);
  }

  private void inliningOfAnd(final String sel) {
    byte[] bytecodes = methodToBytecodes(
        "test = ( true " + sel + " [ #val ] )");

    assertEquals(12, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_GLOBAL,
        new BC(Bytecodes.JUMP_ON_FALSE_POP, 7),
        // true branch
        Bytecodes.PUSH_CONSTANT, // push the `#val`
        new BC(Bytecodes.JUMP, 5),
        // false branch, jump_on_false target, push false
        Bytecodes.PUSH_CONSTANT,
        // target of the jump in the true branch
        Bytecodes.RETURN_SELF);
  }

  @Ignore("TODO")
  @Test
  public void testInliningOfAnd() {
    inliningOfAnd("and:");
    inliningOfAnd("&&");
  }

  private void inliningOfOr(final String sel) {
    byte[] bytecodes = methodToBytecodes(
        "test = ( true " + sel + " [ #val ] )");

    assertEquals(12, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_GLOBAL,
        new BC(Bytecodes.JUMP_ON_TRUE_POP, 7),
        // true branch
        Bytecodes.PUSH_CONSTANT, // push the `#val`
        new BC(Bytecodes.JUMP, 5),
        // false branch, jump_on_false target, push false
        Bytecodes.PUSH_CONSTANT,
        // target of the jump in the true branch
        Bytecodes.RETURN_SELF);
  }

  @Ignore("TODO")
  @Test
  public void testInliningOfOr() {
    inliningOfOr("or:");
    inliningOfOr("||");
  }

  @Ignore("TODO")
  @Test
  public void testFieldReadInlining() {
    addField("field");
    byte[] bytecodes = methodToBytecodes(
        "test = ( true and: [ field ] )");

    assertEquals(11, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_GLOBAL,
        new BC(Bytecodes.JUMP_ON_FALSE_POP, 7),
        // true branch
        Bytecodes.PUSH_FIELD,
        new BC(Bytecodes.JUMP, 4),
        // false branch, jump_on_false target, push false
        Bytecodes.PUSH_CONSTANT,
        // target of the jump in the true branch
        Bytecodes.RETURN_SELF);
  }

  @Ignore("TODO")
  @Test
  public void testInliningOfToDo() {
    byte[] bytecodes = methodToBytecodes(
        "test = ( 1 to: 2 do: [:i | i ] )");

    assertEquals(21, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_CONSTANT,
        Bytecodes.PUSH_CONSTANT,
        -1, // TODO: Bytecodes.DUP_SECOND, // stack: Top[1, 2, 1]
        -1, // TODO new BC(Bytecodes.jump_if_greater, 17), // consume only on jump
        Bytecodes.DUP,
        new BC(Bytecodes.POP_LOCAL, 0, 0), // store the i into the local (arg becomes local
                                           // after inlining)
        new BC(Bytecodes.PUSH_LOCAL, 0, 0), // push the local on the stack as part of the
                                            // block's code
        Bytecodes.POP, // cleanup after block.
        Bytecodes.INC, // increment top, the iteration counter
        -1, // TODO: Bytecodes.nil_local,
        new BC(Bytecodes.JUMP_BACKWARDS, 14), // jump back to the jump_if_greater bytecode
        // jump_if_greater target
        Bytecodes.RETURN_SELF);
  }

  @Ignore("TODO")
  @Test
  public void testToDoBlockBlockInlinedSelf() {
    addField("field");
    byte[] bytecodes = methodToBytecodes(
        "test = (\n"
            + " | l1 l2 |\n"
            + " 1 to: 2 do: [:a |\n"
            + "   l1 do: [:b |\n"
            + "     b ifTrue: [\n"
            + "       a.\n"
            + "       l2 := l2 + 1 ] ] ]\n"
            + ")");

    assertEquals(25, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_CONSTANT,
        Bytecodes.PUSH_CONSTANT,
        -1, // TODO: Bytecodes.DUP_SECOND, // stack: Top[1, 2, 1]
        -1, // TODO new BC(Bytecodes.jump_if_greater, 21), // consume only on jump
        Bytecodes.DUP,
        new BC(Bytecodes.POP_LOCAL, 2, 0), // store the i into the local (arg becomes local
                                           // after inlining)
        new BC(Bytecodes.PUSH_LOCAL, 0, 0), // push the local on the stack as part of the
                                            // block's code
        new BC(Bytecodes.PUSH_BLOCK, 2),
        Bytecodes.SEND,
        Bytecodes.POP, // cleanup after block.
        Bytecodes.INC, // increment top, the iteration counter
        -1, // TODO: Bytecodes.nil_local,
        new BC(Bytecodes.JUMP_BACKWARDS, 10), // jump back to the jump_if_greater bytecode
        // jump_if_greater target
        Bytecodes.RETURN_SELF);

    // TODO: fix bcIdx to the PUSH_BLOCK bc
    SMethod blockMethod = (SMethod) mgenc.getConstant(-1);

    check(
        read(blockMethod.getInvokable(), "expressionOrSequence",
            BytecodeLoopNode.class).getBytecodeArray(),
        t(6, new BC(Bytecodes.PUSH_LOCAL, 2, 1, "local b")),
        Bytecodes.POP);
  }
}
