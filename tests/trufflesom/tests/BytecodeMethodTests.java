package trufflesom.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.oracle.truffle.api.source.Source;

import bd.basic.ProgramDefinitionError;
import trufflesom.compiler.ParserBc;
import trufflesom.interpreter.SomLanguage;
import trufflesom.interpreter.bc.Bytecodes;


public class BytecodeMethodTests extends BytecodeTestSetup {

  private byte[] methodToBytecodes(final String source)
      throws ProgramDefinitionError {
    Source s = SomLanguage.getSyntheticSource(source, "test");

    ParserBc parser = new ParserBc(source, s, probe, universe);
    parser.method(mgenc);
    return mgenc.getBytecodeArray();
  }

  @Test
  public void testEmptyMethodReturnsSelf() throws ProgramDefinitionError {
    byte[] bytecodes = methodToBytecodes("test = ( )");

    assertEquals(1, bytecodes.length);
    assertEquals(Bytecodes.RETURN_SELF, bytecodes[0]);
  }

  @Test
  public void testExplicitReturnSelf() throws ProgramDefinitionError {
    byte[] bytecodes = methodToBytecodes("test = ( ^ self )");

    assertEquals(1, bytecodes.length);
    assertEquals(Bytecodes.RETURN_SELF, bytecodes[0]);
  }

  @Test
  public void testDupPopArgumentPop() throws ProgramDefinitionError {
    byte[] bytecodes = methodToBytecodes("test: arg = ( arg := 1. ^ self )");

    assertEquals(6, bytecodes.length);
    assertEquals(Bytecodes.PUSH_CONSTANT, bytecodes[0]);
    assertEquals(Bytecodes.POP_ARGUMENT, bytecodes[2]);
    assertEquals(Bytecodes.RETURN_SELF, bytecodes[5]);
  }

  @Test
  public void testDupPopArgumentPopImplicitReturnSelf() throws ProgramDefinitionError {
    byte[] bytecodes = methodToBytecodes("test: arg = ( arg := 1 )");

    assertEquals(6, bytecodes.length);
    assertEquals(Bytecodes.PUSH_CONSTANT, bytecodes[0]);
    assertEquals(Bytecodes.POP_ARGUMENT, bytecodes[2]);
    assertEquals(Bytecodes.RETURN_SELF, bytecodes[5]);
  }

  @Test
  public void testDupPopLocalPop() throws ProgramDefinitionError {
    byte[] bytecodes = methodToBytecodes("test = ( | local | local := 1. ^ self )");

    assertEquals(6, bytecodes.length);
    assertEquals(Bytecodes.PUSH_CONSTANT, bytecodes[0]);
    assertEquals(Bytecodes.POP_LOCAL, bytecodes[2]);
    assertEquals(Bytecodes.RETURN_SELF, bytecodes[5]);
  }

  @Test
  public void testDupPopField0Pop() throws ProgramDefinitionError {
    addField("field");
    byte[] bytecodes = methodToBytecodes("test = ( field := 1. ^ self )");

    assertEquals(6, bytecodes.length);
    assertEquals(Bytecodes.PUSH_CONSTANT, bytecodes[0]);
    assertEquals(Bytecodes.POP_FIELD, bytecodes[2]);
    assertEquals(Bytecodes.RETURN_SELF, bytecodes[5]);
  }

  @Test
  public void testDupPopFieldNPop() throws ProgramDefinitionError {
    addField("a");
    addField("b");
    addField("c");
    addField("d");
    addField("e");
    addField("field");
    byte[] bytecodes = methodToBytecodes("test = ( field := 1. ^ self )");

    assertEquals(6, bytecodes.length);
    assertEquals(Bytecodes.PUSH_CONSTANT, bytecodes[0]);
    assertEquals(Bytecodes.POP_FIELD, bytecodes[2]);
    assertEquals(Bytecodes.RETURN_SELF, bytecodes[5]);
  }

  @Test
  public void testDupPopFieldReturnSelf() throws ProgramDefinitionError {
    addField("field");
    byte[] bytecodes = methodToBytecodes("test: val = ( field := val )");

    assertEquals(7, bytecodes.length);
    assertEquals(Bytecodes.PUSH_ARGUMENT, bytecodes[0]);
    assertEquals(Bytecodes.POP_FIELD, bytecodes[3]);
    assertEquals(Bytecodes.RETURN_SELF, bytecodes[6]);
  }

  @Test
  public void testDupPopFieldNReturnSelf() throws ProgramDefinitionError {
    addField("a");
    addField("b");
    addField("c");
    addField("d");
    addField("e");
    addField("field");
    byte[] bytecodes = methodToBytecodes("test: val = ( field := val )");

    assertEquals(7, bytecodes.length);
    assertEquals(Bytecodes.PUSH_ARGUMENT, bytecodes[0]);
    assertEquals(Bytecodes.POP_FIELD, bytecodes[3]);
    assertEquals(Bytecodes.RETURN_SELF, bytecodes[6]);
  }

  @Test
  public void testSendDupPopFieldReturnLocal() throws ProgramDefinitionError {
    addField("field");
    byte[] bytecodes = methodToBytecodes("test = ( ^ field := self method )");

    assertEquals(10, bytecodes.length);
    assertEquals(Bytecodes.PUSH_ARGUMENT, bytecodes[0]);
    assertEquals(Bytecodes.SEND, bytecodes[3]);
    assertEquals(Bytecodes.DUP, bytecodes[5]);
    assertEquals(Bytecodes.POP_FIELD, bytecodes[6]);
    assertEquals(Bytecodes.RETURN_LOCAL, bytecodes[9]);
  }

  @Test
  public void testSendDupPopFieldReturnLocalPeriod() throws ProgramDefinitionError {
    addField("field");
    byte[] bytecodes = methodToBytecodes("test = ( ^ field := self method. )");

    assertEquals(10, bytecodes.length);
    assertEquals(Bytecodes.PUSH_ARGUMENT, bytecodes[0]);
    assertEquals(Bytecodes.SEND, bytecodes[3]);
    assertEquals(Bytecodes.DUP, bytecodes[5]);
    assertEquals(Bytecodes.POP_FIELD, bytecodes[6]);
    assertEquals(Bytecodes.RETURN_LOCAL, bytecodes[9]);
  }

}

/**
 * <pre>




def test_block_dup_pop_field_return_local_dot(cgenc, bgenc):
    add_field(cgenc, "field")
    bytecodes = block_to_bytecodes(bgenc, "[ field := 1. ]")

    assert len(bytecodes) == 6
    assert Bytecodes.push_1 == bytecodes[0]
    assert Bytecodes.dup == bytecodes[1]
    assert Bytecodes.pop_field == bytecodes[2]
    assert Bytecodes.return_local == bytecodes[5]
 * </pre>
 */
