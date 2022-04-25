package trufflesom.tests;

import static org.junit.Assert.assertEquals;
import static trufflesom.vm.SymbolTable.symSelf;
import static trufflesom.vm.SymbolTable.symbolFor;

import org.junit.Ignore;
import org.junit.Test;

import com.oracle.truffle.api.source.Source;

import bdt.basic.ProgramDefinitionError;
import bdt.source.SourceCoordinate;
import trufflesom.compiler.ClassGenerationContext;
import trufflesom.compiler.ParserBc;
import trufflesom.compiler.bc.BytecodeMethodGenContext;
import trufflesom.interpreter.SomLanguage;
import trufflesom.interpreter.bc.Bytecodes;
import trufflesom.interpreter.nodes.bc.BytecodeLoopNode;
import trufflesom.vmobjects.SInvokable.SMethod;


public class BytecodeMethodTests extends BytecodeTestSetup {

  private byte[] methodToBytecodes(final String source) {
    Source s = SomLanguage.getSyntheticSource(source, "test");

    cgenc = new ClassGenerationContext(s, null);
    cgenc.setName(symbolFor("Test"));
    addAllFields();

    mgenc = new BytecodeMethodGenContext(cgenc, probe);
    mgenc.addArgumentIfAbsent(symSelf, SourceCoordinate.create(1, 1));

    ParserBc parser = new ParserBc(source, s, probe);
    try {
      parser.method(mgenc);
    } catch (ProgramDefinitionError e) {
      throw new RuntimeException(e);
    }
    return mgenc.getBytecodeArray();
  }

  private void incDecBytecodes(final String op, final byte bytecode) {
    byte[] bytecodes = methodToBytecodes("test = ( 1 " + op + "  1 )");

    assertEquals(3, bytecodes.length);
    check(bytecodes, Bytecodes.PUSH_1, bytecode, Bytecodes.RETURN_SELF);
  }

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

  @Test
  public void testDupPopArgumentPop() {
    byte[] bytecodes = methodToBytecodes("test: arg = ( arg := 1. ^ self )");

    assertEquals(5, bytecodes.length);
    check(bytecodes, Bytecodes.PUSH_1, Bytecodes.POP_ARGUMENT, Bytecodes.RETURN_SELF);
  }

  @Test
  public void testDupPopArgumentPopImplicitReturnSelf() {
    byte[] bytecodes = methodToBytecodes("test: arg = ( arg := 1 )");

    assertEquals(5, bytecodes.length);
    check(bytecodes, Bytecodes.PUSH_1, Bytecodes.POP_ARGUMENT, Bytecodes.RETURN_SELF);
  }

  @Test
  public void testDupPopLocalPop() {
    byte[] bytecodes = methodToBytecodes("test = ( | local | local := 1. ^ self )");

    assertEquals(3, bytecodes.length);
    check(bytecodes, Bytecodes.PUSH_1, Bytecodes.POP_LOCAL_0, Bytecodes.RETURN_SELF);
  }

  @Test
  public void testDupPopField0Pop() {
    addField("field");
    byte[] bytecodes = methodToBytecodes("test = ( field := 1. ^ self )");

    assertEquals(3, bytecodes.length);
    check(bytecodes, Bytecodes.PUSH_1, Bytecodes.POP_FIELD_0, Bytecodes.RETURN_SELF);
  }

  @Test
  public void testDupPopFieldNPop() {
    addField("a");
    addField("b");
    addField("c");
    addField("d");
    addField("e");
    addField("field");
    byte[] bytecodes = methodToBytecodes("test = ( field := 1. ^ self )");

    assertEquals(5, bytecodes.length);
    check(bytecodes, Bytecodes.PUSH_1, Bytecodes.POP_FIELD, Bytecodes.RETURN_SELF);
  }

  @Test
  public void testDupPopFieldReturnSelf() {
    addField("field");
    byte[] bytecodes = methodToBytecodes("test: val = ( field := val )");

    assertEquals(3, bytecodes.length);
    check(bytecodes, Bytecodes.PUSH_ARG1, Bytecodes.POP_FIELD_0, Bytecodes.RETURN_SELF);
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

    assertEquals(5, bytecodes.length);
    check(bytecodes, Bytecodes.PUSH_ARG1, Bytecodes.POP_FIELD, Bytecodes.RETURN_SELF);
  }

  @Test
  public void testSendDupPopFieldReturnLocal() {
    addField("field");
    byte[] bytecodes = methodToBytecodes("test = ( ^ field := self method )");

    assertEquals(6, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_SELF,
        Bytecodes.SEND,
        Bytecodes.DUP,
        Bytecodes.POP_FIELD_0,
        Bytecodes.RETURN_LOCAL);
  }

  @Test
  public void testSendDupPopFieldReturnLocalPeriod() {
    addField("field");
    byte[] bytecodes = methodToBytecodes("test = ( ^ field := self method. )");

    assertEquals(6, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_SELF,
        Bytecodes.SEND,
        Bytecodes.DUP,
        Bytecodes.POP_FIELD_0,
        Bytecodes.RETURN_LOCAL);
  }

  private void ifTrueWithLiteralReturn(final String literal, final byte bytecode) {
    byte[] bytecodes = methodToBytecodes("test = (\n"
        + "  self method ifTrue: [ " + literal + " ].\n"
        + ")");

    int bcLength = Bytecodes.getBytecodeLength(bytecode);

    assertEquals(8 + bcLength, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_SELF,
        Bytecodes.SEND,
        Bytecodes.JUMP_ON_FALSE_TOP_NIL,
        bytecode,
        Bytecodes.POP,
        Bytecodes.RETURN_SELF);

  }

  @Test
  public void testIfTrueWithLiteralReturn() {
    ifTrueWithLiteralReturn("0", Bytecodes.PUSH_0);
    ifTrueWithLiteralReturn("1", Bytecodes.PUSH_1);
    ifTrueWithLiteralReturn("-10", Bytecodes.PUSH_CONSTANT_2);
    ifTrueWithLiteralReturn("3333", Bytecodes.PUSH_CONSTANT_2);
    ifTrueWithLiteralReturn("'str'", Bytecodes.PUSH_CONSTANT_2);
    ifTrueWithLiteralReturn("#sym", Bytecodes.PUSH_CONSTANT_2);
    ifTrueWithLiteralReturn("1.1", Bytecodes.PUSH_CONSTANT_2);
    ifTrueWithLiteralReturn("-2342.234", Bytecodes.PUSH_CONSTANT_2);
    ifTrueWithLiteralReturn("true", Bytecodes.PUSH_CONSTANT_2);
    ifTrueWithLiteralReturn("false", Bytecodes.PUSH_CONSTANT_2);
    ifTrueWithLiteralReturn("nil", Bytecodes.PUSH_NIL);
    ifTrueWithLiteralReturn("SomeGlobal", Bytecodes.PUSH_GLOBAL);
    ifTrueWithLiteralReturn("[]", Bytecodes.PUSH_BLOCK_NO_CTX);
    ifTrueWithLiteralReturn("[ self ]", Bytecodes.PUSH_BLOCK);
  }

  private void ifTrueWithSomethingAndLiteralReturn(final String literal, final byte bytecode) {
    byte[] bytecodes = methodToBytecodes("test = (\n"
        + "  self method ifTrue: [ #fooBarNonTrivialBlock. " + literal + " ].\n"
        + ")");

    int bcLength = Bytecodes.getBytecodeLength(bytecode);

    assertEquals(10 + bcLength, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_SELF,
        Bytecodes.SEND,
        Bytecodes.JUMP_ON_FALSE_TOP_NIL,
        Bytecodes.PUSH_CONSTANT_2,
        Bytecodes.POP,
        bytecode,
        Bytecodes.POP,
        Bytecodes.RETURN_SELF);

  }

  @Test
  public void testIfTrueWithSomethingAndLiteralReturn() {
    ifTrueWithSomethingAndLiteralReturn("0", Bytecodes.PUSH_0);
    ifTrueWithSomethingAndLiteralReturn("1", Bytecodes.PUSH_1);
    ifTrueWithSomethingAndLiteralReturn("-10", Bytecodes.PUSH_CONSTANT);
    ifTrueWithSomethingAndLiteralReturn("3333", Bytecodes.PUSH_CONSTANT);
    ifTrueWithSomethingAndLiteralReturn("'str'", Bytecodes.PUSH_CONSTANT);
    ifTrueWithSomethingAndLiteralReturn("#sym", Bytecodes.PUSH_CONSTANT);
    ifTrueWithSomethingAndLiteralReturn("1.1", Bytecodes.PUSH_CONSTANT);
    ifTrueWithSomethingAndLiteralReturn("-2342.234", Bytecodes.PUSH_CONSTANT);
    ifTrueWithSomethingAndLiteralReturn("true", Bytecodes.PUSH_CONSTANT);
    ifTrueWithSomethingAndLiteralReturn("false", Bytecodes.PUSH_CONSTANT);
    ifTrueWithSomethingAndLiteralReturn("nil", Bytecodes.PUSH_NIL);
    ifTrueWithSomethingAndLiteralReturn("SomeGlobal", Bytecodes.PUSH_GLOBAL);
    ifTrueWithSomethingAndLiteralReturn("[]", Bytecodes.PUSH_BLOCK_NO_CTX);
    ifTrueWithSomethingAndLiteralReturn("[ self ]", Bytecodes.PUSH_BLOCK);
  }

  private void ifArg(final String selector, final byte jumpBytecode) {
    byte[] bytecodes = methodToBytecodes(
        "test: arg = (\n"
            + "  #start.\n"
            + "  self method " + selector + " [ arg ].\n"
            + "  #end\n"
            + ")");

    assertEquals(13, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_CONSTANT_0,
        Bytecodes.POP,
        Bytecodes.PUSH_SELF,
        Bytecodes.SEND,
        new BC(jumpBytecode, 4),
        Bytecodes.PUSH_ARG1,
        Bytecodes.POP,
        Bytecodes.PUSH_CONSTANT,
        Bytecodes.RETURN_SELF);
  }

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

    assertEquals(14, bytecodes.length);
    check(bytecodes,
        t(4, Bytecodes.SEND),
        new BC(Bytecodes.JUMP_ON_FALSE_TOP_NIL, 4),
        Bytecodes.PUSH_ARG1,
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

    assertEquals(16, bytecodes.length);
    check(bytecodes,
        t(4, Bytecodes.SEND),
        new BC(Bytecodes.JUMP_ON_FALSE_TOP_NIL, 6), // jump to pop bytecode, which is the dot
        new BC(Bytecodes.INC_FIELD_PUSH, 0, 0),
        Bytecodes.POP,
        Bytecodes.PUSH_CONSTANT);
  }

  @Test
  public void testIfTrueAndIncArg() {
    addField("field");
    byte[] bytecodes = methodToBytecodes(
        "test: arg = (\n"
            + "  #start.\n"
            + "  (self key: 5) ifTrue: [ arg + 1 ].\n"
            + "  #end\n"
            + ")");

    assertEquals(15, bytecodes.length);
    check(bytecodes,
        t(4, Bytecodes.SEND),
        new BC(Bytecodes.JUMP_ON_FALSE_TOP_NIL, 5), // jump to pop bytecode, which is the dot
        Bytecodes.PUSH_ARG1,
        Bytecodes.INC,
        Bytecodes.POP,
        Bytecodes.PUSH_CONSTANT);
  }

  private void ifReturnNonLocal(final String ifSelector, final byte jumpBytecode) {
    byte[] bytecodes = methodToBytecodes(
        "test: arg = (\n"
            + "  #start.\n"
            + "  (self key: 5) " + ifSelector + " [ ^ arg ].\n"
            + "  #end\n"
            + ")");

    assertEquals(15, bytecodes.length);
    check(bytecodes,
        t(4, Bytecodes.SEND),
        new BC(jumpBytecode, 5), // jump to pop bytecode, which is the dot
        Bytecodes.PUSH_ARG1,
        Bytecodes.RETURN_LOCAL,
        Bytecodes.POP);
  }

  @Test
  public void testIfReturnNonLocal() {
    ifReturnNonLocal("ifTrue:", Bytecodes.JUMP_ON_FALSE_TOP_NIL);
    ifReturnNonLocal("ifFalse:", Bytecodes.JUMP_ON_TRUE_TOP_NIL);
  }

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

    check(bytecodes,
        Bytecodes.PUSH_CONSTANT_0,
        new BC(Bytecodes.JUMP_ON_FALSE_TOP_NIL, 12),
        Bytecodes.PUSH_CONSTANT_2,
        new BC(Bytecodes.JUMP_ON_TRUE_TOP_NIL, 8),
        Bytecodes.PUSH_FIELD_0,
        Bytecodes.PUSH_ARG1,
        Bytecodes.SEND,
        Bytecodes.RETURN_LOCAL,
        Bytecodes.RETURN_SELF);
  }

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

    assertEquals(47, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_LOCAL_1,
        Bytecodes.POP_LOCAL_0,
        t(3, new BC(Bytecodes.JUMP_ON_FALSE_TOP_NIL, 43)),
        t(7, new BC(Bytecodes.POP_LOCAL, 5, 0)),
        t(12, Bytecodes.POP_LOCAL_2),
        t(15, new BC(Bytecodes.JUMP_ON_TRUE_TOP_NIL, 31)),
        t(19, new BC(Bytecodes.POP_LOCAL, 8, 0)),
        t(40, new BC(Bytecodes.PUSH_LOCAL, 3, 0)));
  }

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

    assertEquals(31, bytecodes.length);

    check(bytecodes,
        // a := 1
        Bytecodes.PUSH_1,
        Bytecodes.POP_LOCAL_0, // a

        Bytecodes.PUSH_CONSTANT_0,
        new BC(Bytecodes.JUMP_ON_FALSE_TOP_NIL, 24),

        // e := 0
        Bytecodes.PUSH_0,
        Bytecodes.POP_LOCAL_2, // e

        t(13, new BC(Bytecodes.JUMP_ON_TRUE_TOP_NIL, 14)),

        Bytecodes.PUSH_1,
        new BC(Bytecodes.POP_LOCAL, 3, 0)); // h

    check(getBytecodesOfBlock(8), // [ a := 1. a ].
        Bytecodes.PUSH_1,
        new BC(Bytecodes.POP_LOCAL, 0, 1, "local a"),
        new BC(Bytecodes.PUSH_LOCAL, 0, 1, "local a"));

    check(getBytecodesOfBlock(20), // [ h + a + e ].
        // this is a bit confusing, but the order in the localsAndOuters array of the block
        // is the same as in the outer method
        new BC(Bytecodes.PUSH_LOCAL, 3, 1, "local h"),
        new BC(Bytecodes.PUSH_LOCAL, 0, 1, "local a"),
        t(8, new BC(Bytecodes.PUSH_LOCAL, 2, 1, "local e")));

    check(getBytecodesOfBlock(28),
        new BC(Bytecodes.PUSH_LOCAL, 0, 1, "local a"));
  }

  @Test
  public void testInliningOfLocals() {
    byte[] bytecodes = methodToBytecodes(
        "test = (\n"
            + " | a b |\n"
            + " a := b := 0.\n"
            + " true ifTrue: [\n"
            + "   | c d |\n"
            + "   c := d := 1.\n"
            + "   [ a := b := c := d := 2 ].\n"
            + "   false ifFalse: [\n"
            + "     | e f |\n"
            + "     e := f := 3.\n"
            + "     [ a := b := c := d := e := f := 4 ] ] ]\n"
            + ")");

    assertEquals(40, bytecodes.length);
    check(bytecodes,
        // a := b := 0.
        Bytecodes.PUSH_0,
        Bytecodes.DUP,
        Bytecodes.DUP,
        Bytecodes.POP_LOCAL_0, // a
        Bytecodes.POP_LOCAL_1, // b
        Bytecodes.POP,

        Bytecodes.PUSH_CONSTANT_0,
        new BC(Bytecodes.JUMP_ON_FALSE_TOP_NIL, 32),

        // c := d := 1.
        Bytecodes.PUSH_1,
        Bytecodes.DUP,
        Bytecodes.DUP,
        Bytecodes.POP_LOCAL_2, // c
        new BC(Bytecodes.POP_LOCAL, 3, 0), // d
        Bytecodes.POP,

        Bytecodes.PUSH_BLOCK,
        Bytecodes.POP,

        Bytecodes.PUSH_CONSTANT,
        new BC(Bytecodes.JUMP_ON_TRUE_TOP_NIL, 16),

        // e := f := 3.
        Bytecodes.PUSH_CONSTANT,
        Bytecodes.DUP,
        Bytecodes.DUP,
        new BC(Bytecodes.POP_LOCAL, 4, 0), // e
        new BC(Bytecodes.POP_LOCAL, 5, 0), // f
        Bytecodes.POP,

        Bytecodes.PUSH_BLOCK,
        Bytecodes.RETURN_SELF);

    check(getBytecodesOfBlock(18),
        // a := b := c := d := 2
        Bytecodes.PUSH_CONSTANT_0,
        Bytecodes.DUP,
        Bytecodes.DUP,
        Bytecodes.DUP,
        Bytecodes.DUP,
        new BC(Bytecodes.POP_LOCAL, 0, 1), // a
        new BC(Bytecodes.POP_LOCAL, 1, 1), // b
        new BC(Bytecodes.POP_LOCAL, 2, 1), // c
        new BC(Bytecodes.POP_LOCAL, 3, 1), // d
        Bytecodes.RETURN_LOCAL);

    check(getBytecodesOfBlock(37),
        // a := b := c := d := e := f := 4
        Bytecodes.PUSH_CONSTANT_0,
        Bytecodes.DUP,
        Bytecodes.DUP,
        Bytecodes.DUP,
        Bytecodes.DUP,
        Bytecodes.DUP,
        Bytecodes.DUP,
        new BC(Bytecodes.POP_LOCAL, 0, 1), // a
        new BC(Bytecodes.POP_LOCAL, 1, 1), // b
        new BC(Bytecodes.POP_LOCAL, 2, 1), // c
        new BC(Bytecodes.POP_LOCAL, 3, 1), // d
        new BC(Bytecodes.POP_LOCAL, 4, 1), // e
        new BC(Bytecodes.POP_LOCAL, 5, 1), // f
        Bytecodes.RETURN_LOCAL);
  }

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

    assertEquals(13, bytecodes.length);
    check(bytecodes,
        t(1, new BC(Bytecodes.JUMP_ON_TRUE_TOP_NIL, 11)),
        new BC(Bytecodes.PUSH_ARG1, "arg a"),
        Bytecodes.POP,
        new BC(Bytecodes.PUSH_LOCAL_0, "local b"),
        Bytecodes.POP,
        new BC(Bytecodes.PUSH_LOCAL_1, "local c"));

    SMethod blockMethod = (SMethod) mgenc.getConstant(10);
    BytecodeLoopNode blockNode =
        read(blockMethod.getInvokable(), "body", BytecodeLoopNode.class);
    byte[] blockBytecodes = blockNode.getBytecodeArray();

    check(blockBytecodes,
        new BC(Bytecodes.PUSH_ARGUMENT, 1, 1, "arg a"),
        t(4, new BC(Bytecodes.PUSH_LOCAL, 0, 1, "local b")),
        t(8, new BC(Bytecodes.PUSH_LOCAL, 1, 1, "local c")),
        t(12, new BC(Bytecodes.PUSH_ARG1, "arg d")));

    blockMethod = (SMethod) blockNode.getConstant(blockBytecodes[14 + 1]);

    check(
        read(blockMethod.getInvokable(), "body",
            BytecodeLoopNode.class).getBytecodeArray(),
        new BC(Bytecodes.PUSH_ARGUMENT, 1, 2, "arg a"),
        t(4, new BC(Bytecodes.PUSH_LOCAL, 0, 2, "local b")),
        t(8, new BC(Bytecodes.PUSH_LOCAL, 1, 2, "local c")),
        t(12, new BC(Bytecodes.PUSH_ARGUMENT, 1, 1, "arg d")),
        t(16, new BC(Bytecodes.PUSH_ARG1, "arg e")));
  }

  @Test
  public void testIfTrueIfFalseArg() {
    byte[] bytecodes = methodToBytecodes(
        "test: arg1 with: arg2 = (\n"
            + " #start.\n"
            + " self method ifTrue: [ arg1 ] ifFalse: [ arg2 ].\n"
            + " #end\n"
            + ")");

    assertEquals(17, bytecodes.length);
    check(bytecodes,
        t(5, new BC(Bytecodes.JUMP_ON_FALSE_POP, 7)),
        Bytecodes.PUSH_ARG1,
        new BC(Bytecodes.JUMP, 4),
        Bytecodes.PUSH_ARG2,
        Bytecodes.POP);
  }

  @Test
  public void testIfTrueIfFalseNlrArg1() {
    byte[] bytecodes = methodToBytecodes(
        "test: arg1 with: arg2 = (\n"
            + "  #start.\n"
            + "  self method ifTrue: [ ^ arg1 ] ifFalse: [ arg2 ].\n"
            + "  #end\n"
            + ")");

    assertEquals(18, bytecodes.length);
    check(bytecodes,
        t(5, new BC(Bytecodes.JUMP_ON_FALSE_POP, 8)),
        Bytecodes.PUSH_ARG1,
        Bytecodes.RETURN_LOCAL,
        new BC(Bytecodes.JUMP, 4),
        Bytecodes.PUSH_ARG2,
        Bytecodes.POP);
  }

  @Test
  public void testIfTrueIfFalseNlrArg2() {
    byte[] bytecodes = methodToBytecodes(
        "test: arg1 with: arg2 = (\n"
            + "  #start.\n"
            + "  self method ifTrue: [ arg1 ] ifFalse: [ ^ arg2 ].\n"
            + "  #end\n"
            + ")");

    assertEquals(18, bytecodes.length);
    check(bytecodes,
        t(5, new BC(Bytecodes.JUMP_ON_FALSE_POP, 7)),
        Bytecodes.PUSH_ARG1,
        new BC(Bytecodes.JUMP, 5),
        Bytecodes.PUSH_ARG2,
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

    assertEquals(15, bytecodes.length);
    check(bytecodes,
        t(5, new BC(jumpBytecode, 8)),
        t(10, new BC(Bytecodes.JUMP, 4)));
  }

  @Test
  public void testIfTrueIfFalseReturn() {
    ifTrueIfFalseReturn("ifTrue:", "ifFalse:", Bytecodes.JUMP_ON_FALSE_POP);
    ifTrueIfFalseReturn("ifFalse:", "ifTrue:", Bytecodes.JUMP_ON_TRUE_POP);
  }

  @Test
  public void testIfPushConsantSame() {
    byte[] bytecodes = methodToBytecodes(
        "test = (\n"
            + "  #a. #b. #c. #d.\n"
            + "  true ifFalse: [ #a. #b. #c. #d. ]\n"
            + ")");

    assertEquals(23, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_CONSTANT_0,
        t(2, Bytecodes.PUSH_CONSTANT_1),
        t(4, Bytecodes.PUSH_CONSTANT_2),
        t(6, Bytecodes.PUSH_CONSTANT),
        t(11, new BC(Bytecodes.JUMP_ON_TRUE_TOP_NIL, 11)),
        t(14, Bytecodes.PUSH_CONSTANT_0),
        t(16, Bytecodes.PUSH_CONSTANT_1),
        t(18, Bytecodes.PUSH_CONSTANT_2),
        t(20, new BC(Bytecodes.PUSH_CONSTANT, 3)));
  }

  @Test
  public void testIfPushConsantDifferent() {
    byte[] bytecodes = methodToBytecodes(
        "test = (\n"
            + "  #a. #b. #c. #d.\n"
            + "  true ifFalse: [ #e. #f. #g. #h. ]\n"
            + ")");

    assertEquals(26, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_CONSTANT_0,
        t(2, Bytecodes.PUSH_CONSTANT_1),
        t(4, Bytecodes.PUSH_CONSTANT_2),
        t(6, Bytecodes.PUSH_CONSTANT),
        t(11, new BC(Bytecodes.JUMP_ON_TRUE_TOP_NIL, 14)),
        t(14, new BC(Bytecodes.PUSH_CONSTANT, 6)),
        t(17, new BC(Bytecodes.PUSH_CONSTANT, 7)),
        t(20, new BC(Bytecodes.PUSH_CONSTANT, 8)),
        t(23, new BC(Bytecodes.PUSH_CONSTANT, 9)));
  }

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

    assertEquals(17, bytecodes.length);
    check(bytecodes,
        t(2, Bytecodes.PUSH_CONSTANT),
        jumpBytecode,
        Bytecodes.PUSH_ARG1,
        Bytecodes.POP,
        new BC(Bytecodes.JUMP_BACKWARDS, 7),
        Bytecodes.PUSH_NIL,
        Bytecodes.POP);
  }

  @Test
  public void testWhileInlining() {
    whileInlining("whileTrue:", Bytecodes.JUMP_ON_FALSE_POP);
    whileInlining("whileFalse:", Bytecodes.JUMP_ON_TRUE_POP);
  }

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
        Bytecodes.PUSH_CONSTANT_0,
        Bytecodes.POP,
        Bytecodes.PUSH_CONSTANT_1,
        Bytecodes.POP,
        Bytecodes.PUSH_CONSTANT_2,
        Bytecodes.POP,
        Bytecodes.PUSH_0,
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

        Bytecodes.PUSH_NIL,
        Bytecodes.POP);
  }

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
        Bytecodes.PUSH_0,
        new BC(Bytecodes.JUMP_ON_FALSE_TOP_NIL, 15,
            "jump to the pop after the if, before pushing #end"),

        Bytecodes.PUSH_1,
        Bytecodes.RETURN_LOCAL,

        new BC(Bytecodes.JUMP_ON_FALSE_POP, 9,
            "jump to push_nil as result of whileTrue"),

        Bytecodes.PUSH_0,
        Bytecodes.RETURN_LOCAL,
        Bytecodes.POP,

        new BC(Bytecodes.JUMP_BACKWARDS, 8,
            "jump to the push_1 of the condition"),
        Bytecodes.PUSH_NIL,
        Bytecodes.POP);
  }

  private void inliningOfAnd(final String sel) {
    byte[] bytecodes = methodToBytecodes(
        "test = ( true " + sel + " [ #val ] )");

    assertEquals(11, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_CONSTANT_0,
        new BC(Bytecodes.JUMP_ON_FALSE_POP, 7),
        // true branch
        Bytecodes.PUSH_CONSTANT_2, // push the `#val`
        new BC(Bytecodes.JUMP, 5),
        // false branch, jump_on_false target, push false
        Bytecodes.PUSH_CONSTANT,
        // target of the jump in the true branch
        Bytecodes.RETURN_SELF);
  }

  @Test
  public void testInliningOfAnd() {
    inliningOfAnd("and:");
    inliningOfAnd("&&");
  }

  private void inliningOfOr(final String sel) {
    byte[] bytecodes = methodToBytecodes(
        "test = ( true " + sel + " [ #val ] )");

    assertEquals(10, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_CONSTANT_0,
        new BC(Bytecodes.JUMP_ON_TRUE_POP, 7),
        // true branch
        Bytecodes.PUSH_CONSTANT_2, // push the `#val`
        new BC(Bytecodes.JUMP, 4),
        // false branch, jump_on_false target, push false
        Bytecodes.PUSH_CONSTANT_0,
        // target of the jump in the true branch
        Bytecodes.RETURN_SELF);
  }

  @Test
  public void testInliningOfOr() {
    inliningOfOr("or:");
    inliningOfOr("||");
  }

  @Test
  public void testFieldReadInlining() {
    addField("field");
    byte[] bytecodes = methodToBytecodes(
        "test = ( true and: [ field ] )");

    assertEquals(10, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_CONSTANT_0,
        new BC(Bytecodes.JUMP_ON_FALSE_POP, 7),
        // true branch
        Bytecodes.PUSH_FIELD_0,
        new BC(Bytecodes.JUMP, 4),
        // false branch, jump_on_false target, push false
        Bytecodes.PUSH_CONSTANT_2,
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
        read(blockMethod.getInvokable(), "body",
            BytecodeLoopNode.class).getBytecodeArray(),
        t(6, new BC(Bytecodes.PUSH_LOCAL, 2, 1, "local b")),
        Bytecodes.POP);
  }

  private void returnFieldTrivial(final String fieldName, final byte bytecode) {
    addField("fieldA");
    addField("fieldB");
    addField("fieldC");
    addField("fieldD");
    byte[] bytecodes = methodToBytecodes("test = ( ^ " + fieldName + " )");

    assertEquals(1, bytecodes.length);
    check(bytecodes,
        bytecode);
  }

  @Test
  public void testReturnFieldTrivial() {
    returnFieldTrivial("fieldA", Bytecodes.RETURN_FIELD_0);
    returnFieldTrivial("fieldB", Bytecodes.RETURN_FIELD_1);
    returnFieldTrivial("fieldC", Bytecodes.RETURN_FIELD_2);
  }

  private void returnFieldLessTrivial(final String fieldName, final byte bytecode) {
    addField("fieldA");
    addField("fieldB");
    addField("fieldC");
    addField("fieldD");
    byte[] bytecodes = methodToBytecodes("test = ( #foo. ^ " + fieldName + " )");

    assertEquals(3, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_CONSTANT_0,
        Bytecodes.POP,
        bytecode);
  }

  @Test
  public void testReturnFieldLessTrivial() {
    returnFieldLessTrivial("fieldA", Bytecodes.RETURN_FIELD_0);
    returnFieldLessTrivial("fieldB", Bytecodes.RETURN_FIELD_1);
    returnFieldLessTrivial("fieldC", Bytecodes.RETURN_FIELD_2);
  }

  private void trivialMethodInlining(final String source, final byte bytecode) {
    byte[] bytecodes = methodToBytecodes("test = ( true ifTrue: [ " + source + " ] )");

    check(bytecodes,
        Bytecodes.PUSH_CONSTANT_0,
        Bytecodes.JUMP_ON_FALSE_TOP_NIL,
        bytecode,
        Bytecodes.RETURN_SELF);
  }

  @Test
  public void testTrivialMethodInlining() {
    trivialMethodInlining("0", Bytecodes.PUSH_0);
    trivialMethodInlining("1", Bytecodes.PUSH_1);
    trivialMethodInlining("-10", Bytecodes.PUSH_CONSTANT_2);
    trivialMethodInlining("'str'", Bytecodes.PUSH_CONSTANT_2);
    trivialMethodInlining("#sym", Bytecodes.PUSH_CONSTANT_2);
    trivialMethodInlining("1.1", Bytecodes.PUSH_CONSTANT_2);
    trivialMethodInlining("-2342.234", Bytecodes.PUSH_CONSTANT_2);
    trivialMethodInlining("true", Bytecodes.PUSH_CONSTANT_0);
    trivialMethodInlining("false", Bytecodes.PUSH_CONSTANT_2);
    trivialMethodInlining("nil", Bytecodes.PUSH_NIL);
    trivialMethodInlining("Nil", Bytecodes.PUSH_GLOBAL);
    trivialMethodInlining("UnknownGlobal", Bytecodes.PUSH_GLOBAL);
    trivialMethodInlining("[]", Bytecodes.PUSH_BLOCK_NO_CTX);
  }

  private void incField(final int field) {
    addField("field0");
    addField("field1");
    addField("field2");
    addField("field3");
    addField("field4");
    addField("field5");
    addField("field6");

    String fieldName = "field" + field;

    byte[] bytecodes = methodToBytecodes(
        "test = ( " + fieldName + " := " + fieldName + " + 1 )");

    assertEquals(4, bytecodes.length);
    check(bytecodes,
        new BC(Bytecodes.INC_FIELD, field, 0),
        Bytecodes.RETURN_SELF);
  }

  @Test
  public void testIncField() {
    for (int i = 0; i < 7; i += 1) {
      incField(i);
    }
  }

  private void incFieldNonTrivial(final int field) {
    addField("field0");
    addField("field1");
    addField("field2");
    addField("field3");
    addField("field4");
    addField("field5");
    addField("field6");

    String fieldName = "field" + field;

    byte[] bytecodes = methodToBytecodes(
        "test = ( 1. " + fieldName + " := " + fieldName + " + 1. 2 )");

    assertEquals(7, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_1, Bytecodes.POP,
        new BC(Bytecodes.INC_FIELD, field, 0),
        Bytecodes.PUSH_CONSTANT_0,
        Bytecodes.RETURN_SELF);
  }

  @Test
  public void testIncFieldNonTrivial() {
    for (int i = 0; i < 7; i += 1) {
      incFieldNonTrivial(i);
    }
  }

  private void returnIncField(final int field) {
    addField("field0");
    addField("field1");
    addField("field2");
    addField("field3");
    addField("field4");
    addField("field5");
    addField("field6");

    String fieldName = "field" + field;

    byte[] bytecodes = methodToBytecodes(
        "test = ( #foo. ^ " + fieldName + " := " + fieldName + " + 1 )");

    assertEquals(6, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_CONSTANT_0,
        Bytecodes.POP,
        new BC(Bytecodes.INC_FIELD_PUSH, field, 0),
        Bytecodes.RETURN_LOCAL);
  }

  @Test
  public void testReturnIncField() {
    for (int i = 0; i < 7; i += 1) {
      returnIncField(i);
    }
  }

  private void returnField(final int fieldNum, final Object bytecode) {
    addField("field0");
    addField("field1");
    addField("field2");
    addField("field3");
    addField("field4");
    addField("field5");
    addField("field6");

    String fieldName = "field" + fieldNum;
    byte[] bytecodes = methodToBytecodes(
        "test = ( 1. ^ " + fieldName + " )");

    check(bytecodes,
        Bytecodes.PUSH_1,
        Bytecodes.POP,
        bytecode);
  }

  @Test
  public void testReturnField() {
    returnField(0, Bytecodes.RETURN_FIELD_0);
    returnField(1, Bytecodes.RETURN_FIELD_1);
    returnField(2, Bytecodes.RETURN_FIELD_2);

    returnField(3, new BC(Bytecodes.PUSH_FIELD, 3));
    returnField(4, new BC(Bytecodes.PUSH_FIELD, 4));
  }
}
