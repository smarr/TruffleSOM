package trufflesom.tests;

import static org.junit.Assert.assertEquals;
import static trufflesom.compiler.bc.Disassembler.dumpMethod;
import static trufflesom.vm.SymbolTable.symSelf;
import static trufflesom.vm.SymbolTable.symbolFor;

import org.junit.Test;

import com.oracle.truffle.api.source.Source;

import bdt.basic.ProgramDefinitionError;
import bdt.source.SourceCoordinate;
import trufflesom.compiler.ClassGenerationContext;
import trufflesom.compiler.ParserBc;
import trufflesom.compiler.bc.BytecodeMethodGenContext;
import trufflesom.interpreter.SomLanguage;
import trufflesom.interpreter.bc.Bytecodes;


public class BytecodeBlockTests extends BytecodeTestSetup {

  protected BytecodeMethodGenContext bgenc;

  @Override
  public void dump() {
    dumpMethod(bgenc);
  }

  private byte[] blockToBytecodes(final String source) {
    Source s = SomLanguage.getSyntheticSource(source, "test");

    cgenc = new ClassGenerationContext(s, null);
    cgenc.setName(symbolFor("Test"));
    addAllFields();

    mgenc = new BytecodeMethodGenContext(cgenc, probe);
    mgenc.addArgumentIfAbsent(symSelf, SourceCoordinate.create(1, 1));

    mgenc.setSignature(symbolFor("outer"));
    mgenc.setVarsOnMethodScope();

    bgenc = new BytecodeMethodGenContext(cgenc, mgenc);

    ParserBc parser = new ParserBc(source, s, probe);
    try {
      parser.nestedBlock(bgenc);
    } catch (ProgramDefinitionError e) {
      throw new RuntimeException(e);
    }
    return bgenc.getBytecodeArray();
  }

  private byte[] blockToBytecodes(final String source, final String outerMethodArgName) {
    Source s = SomLanguage.getSyntheticSource(source, "test");

    cgenc = new ClassGenerationContext(s, null);
    cgenc.setName(symbolFor("Test"));
    addAllFields();

    mgenc = new BytecodeMethodGenContext(cgenc, probe);
    mgenc.addArgumentIfAbsent(symSelf, SourceCoordinate.create(1, 1));
    mgenc.addArgumentIfAbsent(
        symbolFor(outerMethodArgName), SourceCoordinate.create(2, 1));

    mgenc.setSignature(symbolFor("outer"));
    mgenc.setVarsOnMethodScope();

    bgenc = new BytecodeMethodGenContext(cgenc, mgenc);

    ParserBc parser = new ParserBc(source, s, probe);
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

    assertEquals(15, bytecodes.length);
    check(bytecodes,
        t(5, Bytecodes.SEND),
        new BC(Bytecodes.JUMP_ON_FALSE_TOP_NIL, 4),
        Bytecodes.PUSH_ARG1,
        Bytecodes.POP,
        Bytecodes.PUSH_CONSTANT);
  }

  @Test
  public void testBlockIfTrueMethodArg() {
    byte[] bytecodes = blockToBytecodes(
        "[ #start.\n"
            + " self method ifTrue: [ arg ].\n"
            + " #end\n"
            + "]",
        "arg");

    assertEquals(17, bytecodes.length);
    check(bytecodes,
        t(7, new BC(Bytecodes.JUMP_ON_FALSE_TOP_NIL, 6)),
        new BC(Bytecodes.PUSH_ARGUMENT, 1, 1),
        Bytecodes.POP,
        Bytecodes.PUSH_CONSTANT);
  }

  private void blockIfReturnNonLocal(final String sel, final byte jumpBytecode) {
    byte[] bytecodes = blockToBytecodes("[:arg |\n"
        + " #start.\n"
        + " self method " + sel + " [ ^ arg ].\n"
        + " #end\n"
        + "]");

    assertEquals(17, bytecodes.length);
    check(bytecodes,
        t(5, Bytecodes.SEND),
        new BC(jumpBytecode, 6),
        Bytecodes.PUSH_ARG1,
        new BC(Bytecodes.RETURN_NON_LOCAL, 1),
        Bytecodes.POP);
  }

  @Test
  public void testBlockIfReturnNonLocal() {
    blockIfReturnNonLocal("ifTrue:", Bytecodes.JUMP_ON_FALSE_TOP_NIL);
    blockIfReturnNonLocal("ifFalse:", Bytecodes.JUMP_ON_TRUE_TOP_NIL);
  }
}
