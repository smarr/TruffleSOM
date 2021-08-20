package trufflesom.tests;

import static org.junit.Assert.assertEquals;
import static trufflesom.compiler.bc.Disassembler.dumpMethod;
import static trufflesom.vm.SymbolTable.symbolFor;

import org.junit.Test;

import com.oracle.truffle.api.source.Source;

import bd.basic.ProgramDefinitionError;
import trufflesom.compiler.ParserBc;
import trufflesom.compiler.bc.BytecodeMethodGenContext;
import trufflesom.interpreter.SomLanguage;
import trufflesom.interpreter.bc.Bytecodes;


public class BytecodeBlockTests extends BytecodeTestSetup {

  protected BytecodeMethodGenContext bgenc;

  public BytecodeBlockTests() {
    mgenc.setSignature(symbolFor("outer"));
    mgenc.setVarsOnMethodScope();

    initBgenc();
  }

  private void initBgenc() {
    bgenc = new BytecodeMethodGenContext(cgenc, mgenc);
  }

  @Override
  public void dump() {
    dumpMethod(bgenc);
  }

  private byte[] blockToBytecodes(final String source) {
    Source s = SomLanguage.getSyntheticSource(source, "test");

    ParserBc parser = new ParserBc(source, s, probe, universe);
    try {
      parser.nestedBlock(bgenc);
    } catch (ProgramDefinitionError e) {
      throw new RuntimeException(e);
    }
    return bgenc.getBytecodeArray();
  }

  @Test
  public void testDupPopArgumentPop() {
    byte[] bytecodes = blockToBytecodes("[:arg | arg := 1. arg ]");

    assertEquals(6, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_1,
        Bytecodes.POP_ARGUMENT,
        Bytecodes.PUSH_ARG1,
        Bytecodes.RETURN_LOCAL);
  }

  @Test
  public void testDupPopArgumentPopImplicitReturn() {
    byte[] bytecodes = blockToBytecodes("[:arg | arg := 1 ]");

    assertEquals(6, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_1,
        Bytecodes.DUP,
        Bytecodes.POP_ARGUMENT,
        Bytecodes.RETURN_LOCAL);
  }

  @Test
  public void testDupPopArgumentPopImplicitReturnDot() {
    byte[] bytecodes = blockToBytecodes("[:arg | arg := 1. ]");

    assertEquals(6, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_1,
        Bytecodes.DUP,
        Bytecodes.POP_ARGUMENT,
        Bytecodes.RETURN_LOCAL);
  }

  @Test
  public void testDupPopLocalReturnLocal() {
    byte[] bytecodes = blockToBytecodes("[| local | local := 1 ]");

    assertEquals(4, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_1,
        Bytecodes.DUP,
        Bytecodes.POP_LOCAL_0,
        Bytecodes.RETURN_LOCAL);
  }

  @Test
  public void testDupPopFieldReturnLocal() {
    addField("field");
    byte[] bytecodes = blockToBytecodes("[ field := 1 ]");

    assertEquals(6, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_1,
        Bytecodes.DUP,
        Bytecodes.POP_FIELD,
        Bytecodes.RETURN_LOCAL);
  }

  @Test
  public void testDupPopFieldReturnLocalDot() {
    addField("field");
    byte[] bytecodes = blockToBytecodes("[ field := 1. ]");

    assertEquals(6, bytecodes.length);
    check(bytecodes,
        Bytecodes.PUSH_1,
        Bytecodes.DUP,
        Bytecodes.POP_FIELD,
        Bytecodes.RETURN_LOCAL);
  }

  @Test
  public void testBlockIfTrueArg() {
    byte[] bytecodes = blockToBytecodes(
        "[:arg | #start.\n"
            + " self method ifTrue: [ arg ].\n"
            + " #end\n"
            + "]");

    assertEquals(17, bytecodes.length);
    check(bytecodes,
        t(5, Bytecodes.SEND),
        new BC(Bytecodes.JUMP_ON_FALSE_TOP_NIL, 6),
        new BC(Bytecodes.PUSH_ARGUMENT, 1, 0),
        Bytecodes.POP,
        Bytecodes.PUSH_CONSTANT);
  }

  @Test
  public void testBlockIfTrueMethodArg() {
    Source s = SomLanguage.getSyntheticSource("test arg", "arg");
    mgenc.addArgumentIfAbsent(symbolFor("arg"), s.createSection(1));

    byte[] bytecodes = blockToBytecodes(
        "[ #start.\n"
            + " self method ifTrue: [ arg ].\n"
            + " #end\n"
            + "]");

    assertEquals(17, bytecodes.length);
    check(bytecodes,
        t(7, new BC(Bytecodes.JUMP_ON_FALSE_TOP_NIL, 6)),
        new BC(Bytecodes.PUSH_ARGUMENT, 1, 1),
        Bytecodes.POP,
        Bytecodes.PUSH_CONSTANT);
  }

  private void blockIfReturnNonLocal(final String sel, final byte jumpBytecode) {
    initBgenc();
    byte[] bytecodes = blockToBytecodes("[:arg |\n"
        + " #start.\n"
        + " self method " + sel + " [ ^ arg ].\n"
        + " #end\n"
        + "]");

    assertEquals(19, bytecodes.length);
    check(bytecodes,
        t(5, Bytecodes.SEND),
        new BC(jumpBytecode, 8),
        new BC(Bytecodes.PUSH_ARGUMENT, 1, 0),
        new BC(Bytecodes.RETURN_NON_LOCAL, 1),
        Bytecodes.POP);
  }

  @Test
  public void testBlockIfReturnNonLocal() {
    blockIfReturnNonLocal("ifTrue:", Bytecodes.JUMP_ON_FALSE_TOP_NIL);
    blockIfReturnNonLocal("ifFalse:", Bytecodes.JUMP_ON_TRUE_TOP_NIL);
  }

}
