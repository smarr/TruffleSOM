package trufflesom.tests;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static trufflesom.vm.SymbolTable.symSelf;
import static trufflesom.vm.SymbolTable.symbolFor;

import org.junit.Test;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import bd.basic.ProgramDefinitionError;
import bd.primitives.nodes.PreevaluatedExpression;
import trufflesom.compiler.MethodGenerationContext;
import trufflesom.compiler.ParserAst;
import trufflesom.compiler.ParserBc;
import trufflesom.compiler.bc.BytecodeMethodGenContext;
import trufflesom.interpreter.Method;
import trufflesom.interpreter.SomLanguage;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.FieldNode.FieldReadNode;
import trufflesom.interpreter.nodes.FieldNode.WriteAndReturnSelf;
import trufflesom.interpreter.nodes.GlobalNode;
import trufflesom.interpreter.nodes.GlobalNode.FalseGlobalNode;
import trufflesom.interpreter.nodes.GlobalNode.NilGlobalNode;
import trufflesom.interpreter.nodes.GlobalNode.TrueGlobalNode;
import trufflesom.interpreter.nodes.literals.DoubleLiteralNode;
import trufflesom.interpreter.nodes.literals.IntegerLiteralNode;
import trufflesom.interpreter.nodes.literals.StringLiteralNode;
import trufflesom.interpreter.nodes.literals.SymbolLiteralNode;
import trufflesom.vm.VmSettings;
import trufflesom.vmobjects.SInvokable;


public class OptimizeTrivialTests extends TruffleTestSetup {

  protected MethodGenerationContext mgenc;
  protected MethodGenerationContext bgenc;

  public OptimizeTrivialTests() {
    super();
    initMgenc();
  }

  protected void initMgenc() {
    if (VmSettings.UseAstInterp) {
      mgenc = new MethodGenerationContext(cgenc, probe);
    } else {
      mgenc = new BytecodeMethodGenContext(cgenc, probe);
    }
    mgenc.addArgumentIfAbsent(symSelf, sourceForTests.createSection(1, 1));
    fieldCount = 0;
  }

  private void initBgenc() {
    initMgenc();
    mgenc.setSignature(symbolFor("outer"));
    mgenc.setVarsOnMethodScope();
    if (VmSettings.UseAstInterp) {
      bgenc = new MethodGenerationContext(cgenc, mgenc);
    } else {
      bgenc = new BytecodeMethodGenContext(cgenc, mgenc);
    }
  }

  private Method parseMethod(final String source) {
    Source s = SomLanguage.getSyntheticSource(source, "test");
    SourceSection section = s.createSection(1);

    SInvokable ivkbl;

    try {
      if (VmSettings.UseAstInterp) {
        ParserAst parser = new ParserAst(source, s, null, universe);
        ExpressionNode body = parser.method(mgenc);
        ivkbl = mgenc.assemble(body, section, section);
      } else {
        ParserBc parser = new ParserBc(source, s, probe, universe);
        parser.method((BytecodeMethodGenContext) mgenc);
        ivkbl = mgenc.assemble(null, section, section);
      }
      return (Method) ivkbl.getInvokable();
    } catch (ProgramDefinitionError e) {
      throw new RuntimeException(e);
    }
  }

  private Method parseBlock(final String source) {
    Source s = SomLanguage.getSyntheticSource(source, "test");
    SourceSection section = s.createSection(1);

    SInvokable ivkbl;

    try {
      if (VmSettings.UseAstInterp) {
        ParserAst parser = new ParserAst(source, s, null, universe);
        ExpressionNode body = parser.nestedBlock(bgenc);
        ivkbl = bgenc.assemble(body, section, section);
      } else {
        ParserBc parser = new ParserBc(source, s, probe, universe);
        parser.nestedBlock((BytecodeMethodGenContext) bgenc);
        ivkbl = bgenc.assemble(null, section, section);
      }
      return (Method) ivkbl.getInvokable();
    } catch (ProgramDefinitionError e) {
      throw new RuntimeException(e);
    }
  }

  private void literalReturn(final String source, final String result, final Class<?> cls) {
    initMgenc();
    Method m = parseMethod("test = ( ^ " + source + " )");

    assertTrue(m.isTrivial());
    PreevaluatedExpression e = m.copyTrivialNode();
    Object actual = e.doPreEvaluated(null, null);

    assertEquals(result, actual.toString());
    assertThat(e, instanceOf(cls));
  }

  @Test
  public void testLiteralReturn() {
    literalReturn("0", "0", IntegerLiteralNode.class);
    literalReturn("1", "1", IntegerLiteralNode.class);
    literalReturn("-10", "-10", IntegerLiteralNode.class);
    literalReturn("3333", "3333", IntegerLiteralNode.class);
    literalReturn("'str'", "str", StringLiteralNode.class);
    literalReturn("#sym", "#sym", SymbolLiteralNode.class);
    literalReturn("1.1", "1.1", DoubleLiteralNode.class);
    literalReturn("-2342.234", "-2342.234", DoubleLiteralNode.class);
    literalReturn("true", "true", TrueGlobalNode.class);
    literalReturn("false", "false", FalseGlobalNode.class);
    literalReturn("nil", "nil", NilGlobalNode.class);
  }

  private void globalReturn(final String source) {
    initMgenc();
    Method m = parseMethod("test = ( ^ " + source + " )");

    assertTrue(m.isTrivial());
    PreevaluatedExpression e = m.copyTrivialNode();
    assertThat(e, instanceOf(GlobalNode.class));
  }

  @Test
  public void testGlobalReturn() {
    globalReturn("Nil");
    globalReturn("system");
    globalReturn("MyClassFooBar");
  }

  @Test
  public void testFieldGetter0() {
    addField("field");
    Method m = parseMethod("test = ( ^ field )");

    assertTrue(m.isTrivial());
    PreevaluatedExpression e = m.copyTrivialNode();
    assertThat(e, instanceOf(FieldReadNode.class));
  }

  @Test
  public void testFieldGetterN() {
    addField("a");
    addField("b");
    addField("c");
    addField("d");
    addField("e");
    addField("field");
    Method m = parseMethod("test = ( ^ field )");

    assertTrue(m.isTrivial());
    PreevaluatedExpression e = m.copyTrivialNode();
    assertThat(e, instanceOf(FieldReadNode.class));
  }

  private void fieldSetter(final String source, final int numExtraFields) {
    initMgenc();
    for (int i = 0; i < numExtraFields; i += 1) {
      addField("f" + i);
    }

    addField("field");

    Method m = parseMethod("test: val = ( " + source + " )");

    assertTrue(m.isTrivial());
    PreevaluatedExpression e = m.copyTrivialNode();
    assertThat(e, instanceOf(WriteAndReturnSelf.class));
  }

  @Test
  public void testFieldSetter() {
    fieldSetter("field := val", 0);
    fieldSetter("field := val", 5);

    fieldSetter("field := val.", 0);
    fieldSetter("field := val.", 5);

    fieldSetter("field := val. ^ self", 0);
    fieldSetter("field := val. ^ self", 5);
  }

  private void literalNoReturn(final String source) {
    initMgenc();
    Method m = parseMethod("test = ( " + source + " )");

    assertFalse(m.isTrivial());
  }

  @Test
  public void testLiteralNoReturn() {
    literalNoReturn("0");
    literalNoReturn("1");
    literalNoReturn("-10");
    literalNoReturn("'str'");
    literalNoReturn("#sym");
    literalNoReturn("1.1");
    literalNoReturn("-2342.234");
    literalNoReturn("true");
    literalNoReturn("false");
    literalNoReturn("nil");
  }

  @Test
  public void testBlockReturn() {
    Method m = parseMethod("test = ( ^ [] )");

    assertFalse(m.isTrivial());
  }

  private void literalBlock(final String source, final String result, final Class<?> cls) {
    initBgenc();
    Method m = parseBlock("[ " + source + " ]");

    assertTrue(m.isTrivial());
    PreevaluatedExpression e = m.copyTrivialNode();
    Object actual = e.doPreEvaluated(null, null);

    assertEquals(result, actual.toString());
    assertThat(e, instanceOf(cls));
  }

  @Test
  public void testLiteralBlock() {
    literalBlock("0", "0", IntegerLiteralNode.class);
    literalBlock("1", "1", IntegerLiteralNode.class);
    literalBlock("-10", "-10", IntegerLiteralNode.class);
    literalBlock("3333", "3333", IntegerLiteralNode.class);
    literalBlock("'str'", "str", StringLiteralNode.class);
    literalBlock("#sym", "#sym", SymbolLiteralNode.class);
    literalBlock("1.1", "1.1", DoubleLiteralNode.class);
    literalBlock("-2342.234", "-2342.234", DoubleLiteralNode.class);
    literalBlock("true", "true", TrueGlobalNode.class);
    literalBlock("false", "false", FalseGlobalNode.class);
    literalBlock("nil", "nil", NilGlobalNode.class);
  }

  @Test
  public void testUnknownGlobalInBlock() {
    initBgenc();
    Method m = parseBlock("[ UnknownGlobalSSSS ]");
    assertFalse(m.isTrivial());
  }
}
