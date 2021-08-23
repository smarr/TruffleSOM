package trufflesom.tests;

import static org.junit.Assert.assertEquals;
import static trufflesom.compiler.bc.Disassembler.dumpMethod;
import static trufflesom.vm.SymbolTable.symBlockSelf;
import static trufflesom.vm.SymbolTable.symbolFor;

import org.junit.Test;

import com.oracle.truffle.api.source.Source;

import bd.basic.ProgramDefinitionError;
import trufflesom.compiler.ParserBc;
import trufflesom.compiler.bc.BytecodeMethodGenContext;
import trufflesom.interpreter.SomLanguage;
import trufflesom.interpreter.bc.Bytecodes;


public class BytecodeBlockTests extends BytecodeTestSetup {

  protected final BytecodeMethodGenContext bgenc;

  public BytecodeBlockTests() {
    mgenc.setSignature(symbolFor("outer"));

    bgenc = new BytecodeMethodGenContext(cgenc, mgenc);
    bgenc.addArgumentIfAbsent(symBlockSelf, null);
  }

  @Override
  public void dump() {
    dumpMethod(bgenc);
  }

  private byte[] blockToBytecodes(final String source)
      throws ProgramDefinitionError {
    Source s = SomLanguage.getSyntheticSource(source, "test");

    ParserBc parser = new ParserBc(source, s, probe, universe);
    parser.nestedBlock(bgenc);
    return bgenc.getBytecodeArray();
  }

  @Test
  public void testDupPopArgumentPop() throws ProgramDefinitionError {
    byte[] bytecodes = blockToBytecodes("[:arg | arg := 1. arg ]");

    assertEquals(9, bytecodes.length);
    assertEquals(Bytecodes.PUSH_CONSTANT, bytecodes[0]);
    assertEquals(Bytecodes.POP_ARGUMENT, bytecodes[2]);
    assertEquals(Bytecodes.PUSH_ARGUMENT, bytecodes[5]);
    assertEquals(Bytecodes.RETURN_LOCAL, bytecodes[8]);
  }

  @Test
  public void testDupPopArgumentPopImplicitReturn() throws ProgramDefinitionError {
    byte[] bytecodes = blockToBytecodes("[:arg | arg := 1 ]");

    assertEquals(7, bytecodes.length);
    assertEquals(Bytecodes.PUSH_CONSTANT, bytecodes[0]);
    assertEquals(Bytecodes.DUP, bytecodes[2]);
    assertEquals(Bytecodes.POP_ARGUMENT, bytecodes[3]);
    assertEquals(Bytecodes.RETURN_LOCAL, bytecodes[6]);
  }

  @Test
  public void testDupPopArgumentPopImplicitReturnDot() throws ProgramDefinitionError {
    byte[] bytecodes = blockToBytecodes("[:arg | arg := 1. ]");

    assertEquals(7, bytecodes.length);
    assertEquals(Bytecodes.PUSH_CONSTANT, bytecodes[0]);
    assertEquals(Bytecodes.DUP, bytecodes[2]);
    assertEquals(Bytecodes.POP_ARGUMENT, bytecodes[3]);
    assertEquals(Bytecodes.RETURN_LOCAL, bytecodes[6]);
  }

  @Test
  public void testDupPopLocalReturnLocal() throws ProgramDefinitionError {
    byte[] bytecodes = blockToBytecodes("[| local | local := 1 ]");

    assertEquals(7, bytecodes.length);
    assertEquals(Bytecodes.PUSH_CONSTANT, bytecodes[0]);
    assertEquals(Bytecodes.DUP, bytecodes[2]);
    assertEquals(Bytecodes.POP_LOCAL, bytecodes[3]);
    assertEquals(Bytecodes.RETURN_LOCAL, bytecodes[6]);
  }

  @Test
  public void testDupPopFieldReturnLocal() throws ProgramDefinitionError {
    addField("field");
    byte[] bytecodes = blockToBytecodes("[ field := 1 ]");

    assertEquals(7, bytecodes.length);
    assertEquals(Bytecodes.PUSH_CONSTANT, bytecodes[0]);
    assertEquals(Bytecodes.DUP, bytecodes[2]);
    assertEquals(Bytecodes.POP_FIELD, bytecodes[3]);
    assertEquals(Bytecodes.RETURN_LOCAL, bytecodes[6]);
  }

  @Test
  public void testDupPopFieldReturnLocalDot() throws ProgramDefinitionError {
    addField("field");
    byte[] bytecodes = blockToBytecodes("[ field := 1. ]");

    assertEquals(7, bytecodes.length);
    assertEquals(Bytecodes.PUSH_CONSTANT, bytecodes[0]);
    assertEquals(Bytecodes.DUP, bytecodes[2]);
    assertEquals(Bytecodes.POP_FIELD, bytecodes[3]);
    assertEquals(Bytecodes.RETURN_LOCAL, bytecodes[6]);
  }
}
