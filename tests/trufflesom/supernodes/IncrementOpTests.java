package trufflesom.supernodes;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgumentReadNode;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.SequenceNode;
import trufflesom.interpreter.nodes.literals.BlockNode;
import trufflesom.interpreter.nodes.specialized.IfInlinedLiteralNode;
import trufflesom.interpreter.supernodes.inc.IncExpWithValueNode;
import trufflesom.interpreter.supernodes.inc.IncLocalVarWithExpNode;
import trufflesom.interpreter.supernodes.inc.IncLocalVarWithValueNode;
import trufflesom.interpreter.supernodes.inc.IncNonLocalVarWithExpNode;
import trufflesom.interpreter.supernodes.inc.IncNonLocalVarWithValueNode;
import trufflesom.interpreter.supernodes.inc.UninitIncFieldWithExpNode;
import trufflesom.interpreter.supernodes.inc.UninitIncFieldWithValueNode;
import trufflesom.tests.AstTestSetup;


public class IncrementOpTests extends AstTestSetup {

  private void basicAddOrSubtract(final String test, final long literalValue,
      final Class<?> nodeType) {
    addField("field");
    SequenceNode seq = (SequenceNode) parseMethod(
        "test: arg = ( | var | \n" + test + " )");

    ExpressionNode testExpr = read(seq, "expressions", 0);
    assertThat(testExpr, instanceOf(nodeType));
    long value = read(testExpr, "incValue", Long.class);
    assertEquals(literalValue, value);
  }

  @Test
  public void testBasicAddOrSubtract() {
    // int const + int const
    basicAddOrSubtract("1 + 1", 1, IncExpWithValueNode.class);
    basicAddOrSubtract("1 + 2", 2, IncExpWithValueNode.class);
    basicAddOrSubtract("1 + 150", 150, IncExpWithValueNode.class);

    // int const - int const
    basicAddOrSubtract("1 - 1", -1, IncExpWithValueNode.class);
    basicAddOrSubtract("1 - 2", -2, IncExpWithValueNode.class);
    basicAddOrSubtract("1 - 150", -150, IncExpWithValueNode.class);

    // int expr + int const
    basicAddOrSubtract("(3 / 4) + 1", 1, IncExpWithValueNode.class);
    basicAddOrSubtract("(3 / 5) + 2", 2, IncExpWithValueNode.class);
    basicAddOrSubtract("(4 / 4) + 150", 150, IncExpWithValueNode.class);

    // int expr - int const
    basicAddOrSubtract("(3 / 4) - 1", -1, IncExpWithValueNode.class);
    basicAddOrSubtract("(3 / 5) - 2", -2, IncExpWithValueNode.class);
    basicAddOrSubtract("(4 / 4) - 150", -150, IncExpWithValueNode.class);

    basicAddOrSubtract("arg + 123", 123, IncExpWithValueNode.class);
    basicAddOrSubtract("var + 245", 245, IncExpWithValueNode.class);
    basicAddOrSubtract("field + 645", 645, IncExpWithValueNode.class);

    basicAddOrSubtract("arg - 123", -123, IncExpWithValueNode.class);
    basicAddOrSubtract("var - 245", -245, IncExpWithValueNode.class);
    basicAddOrSubtract("field - 645", -645, IncExpWithValueNode.class);
  }

  @Test
  public void testIfTrueAndIncArg() {
    SequenceNode seq = (SequenceNode) parseMethod(
        "test: arg = (\n"
            + "#start.\n"
            + "(self key: 5) ifTrue: [ arg + 1 ]. #end )");

    IfInlinedLiteralNode ifNode = (IfInlinedLiteralNode) read(seq, "expressions", 1);

    IncExpWithValueNode inc = read(ifNode, "bodyNode", IncExpWithValueNode.class);
    LocalArgumentReadNode arg = (LocalArgumentReadNode) inc.getRcvr();
    assertEquals(1, arg.argumentIndex);
    assertEquals("arg", arg.getInvocationIdentifier());
  }

  @Test
  public void testFieldInc() {
    basicAddOrSubtract("field := field + 1", 1, UninitIncFieldWithValueNode.class);
    basicAddOrSubtract("field := field - 1", -1, UninitIncFieldWithValueNode.class);
    basicAddOrSubtract("field := field + 1123", 1123, UninitIncFieldWithValueNode.class);
    basicAddOrSubtract("field := field - 234234", -234234, UninitIncFieldWithValueNode.class);
  }

  private void incWithExpr(final String test, final Class<?> nodeType) {
    addField("field");
    SequenceNode seq = (SequenceNode) parseMethod(
        "test: arg = ( | var | \n" + test + " )");

    ExpressionNode testExpr = read(seq, "expressions", 0);
    assertThat(testExpr, instanceOf(nodeType));
  }

  @Test
  public void testFieldIncWithExpression() {
    incWithExpr("field := field + (23 + 434)", UninitIncFieldWithExpNode.class);
    incWithExpr("field := field + var", UninitIncFieldWithExpNode.class);
    incWithExpr("field := field + arg", UninitIncFieldWithExpNode.class);
  }

  @Test
  public void testLocalIncWithExpression() {
    incWithExpr("var := var + (23 + 434)", IncLocalVarWithExpNode.class);
    incWithExpr("var := var + var", IncLocalVarWithExpNode.class);
    incWithExpr("var := var + arg", IncLocalVarWithExpNode.class);

    incWithExpr("var := (23 + 434) + var", IncLocalVarWithExpNode.class);
    incWithExpr("var := var + var", IncLocalVarWithExpNode.class);
    incWithExpr("var := arg + var", IncLocalVarWithExpNode.class);
  }

  @Test
  public void testLocalInc() {
    basicAddOrSubtract("var := var + 1", 1, IncLocalVarWithValueNode.class);
    basicAddOrSubtract("var := var - 1", -1, IncLocalVarWithValueNode.class);
    basicAddOrSubtract("var := var + 1123", 1123, IncLocalVarWithValueNode.class);
    basicAddOrSubtract("var := var - 234234", -234234, IncLocalVarWithValueNode.class);
  }

  private void inBlock(final String test, final long literalValue,
      final Class<?> nodeType) {
    addField("field");
    SequenceNode seq = (SequenceNode) parseMethod(
        "test: arg = ( | var | \n" + test + " )");

    BlockNode block = (BlockNode) read(seq, "expressions", 0);
    ExpressionNode testExpr =
        read(block.getMethod().getInvokable(), "body", ExpressionNode.class);
    assertThat(testExpr, instanceOf(nodeType));
    long value = read(testExpr, "incValue", Long.class);
    assertEquals(literalValue, value);
  }

  @Test
  public void testNonLocalInc() {
    inBlock("[ var := var + 1 ]", 1, IncNonLocalVarWithValueNode.class);
    inBlock("[ var := var - 1 ]", -1, IncNonLocalVarWithValueNode.class);
    inBlock("[ var := var + 1123 ]", 1123, IncNonLocalVarWithValueNode.class);
    inBlock("[ var := var - 234234 ]", -234234, IncNonLocalVarWithValueNode.class);
  }

  private void incWithExprInBlock(final String test, final Class<?> nodeType) {
    addField("field");
    SequenceNode seq = (SequenceNode) parseMethod(
        "test: arg = ( | var | \n" + test + " )");

    BlockNode block = (BlockNode) read(seq, "expressions", 0);
    ExpressionNode testExpr =
        read(block.getMethod().getInvokable(), "body", ExpressionNode.class);
    assertThat(testExpr, instanceOf(nodeType));
  }

  @Test
  public void testNonLocalIncWithExpression() {
    incWithExprInBlock("[ var := var + (23 + 434) ]", IncNonLocalVarWithExpNode.class);
    incWithExprInBlock("[ var := var + var ]", IncNonLocalVarWithExpNode.class);
    incWithExprInBlock("[ var := var + arg ]", IncNonLocalVarWithExpNode.class);

    incWithExprInBlock("[ var := (23 + 434) + var ]", IncNonLocalVarWithExpNode.class);
    incWithExprInBlock("[ var := var + var ]", IncNonLocalVarWithExpNode.class);
    incWithExprInBlock("[ var := arg + var ]", IncNonLocalVarWithExpNode.class);
  }
}
