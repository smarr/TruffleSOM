package trufflesom.supernodes;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgumentReadNode;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.SequenceNode;
import trufflesom.interpreter.nodes.literals.BlockNode;
import trufflesom.interpreter.nodes.specialized.IfInlinedLiteralNode;
import trufflesom.interpreter.supernodes.IntIncrementNode;
import trufflesom.interpreter.supernodes.IntUninitIncFieldNode;
import trufflesom.interpreter.supernodes.LocalVariableIncNode;
import trufflesom.interpreter.supernodes.NonLocalVariableIncNode;
import trufflesom.interpreter.supernodes.UninitIncFieldNode;
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
    basicAddOrSubtract("1 + 1", 1, IntIncrementNode.class);
    basicAddOrSubtract("1 + 2", 2, IntIncrementNode.class);
    basicAddOrSubtract("1 + 150", 150, IntIncrementNode.class);

    // int const - int const
    basicAddOrSubtract("1 - 1", -1, IntIncrementNode.class);
    basicAddOrSubtract("1 - 2", -2, IntIncrementNode.class);
    basicAddOrSubtract("1 - 150", -150, IntIncrementNode.class);

    // int expr + int const
    basicAddOrSubtract("(3 / 4) + 1", 1, IntIncrementNode.class);
    basicAddOrSubtract("(3 / 5) + 2", 2, IntIncrementNode.class);
    basicAddOrSubtract("(4 / 4) + 150", 150, IntIncrementNode.class);

    // int expr - int const
    basicAddOrSubtract("(3 / 4) - 1", -1, IntIncrementNode.class);
    basicAddOrSubtract("(3 / 5) - 2", -2, IntIncrementNode.class);
    basicAddOrSubtract("(4 / 4) - 150", -150, IntIncrementNode.class);

    basicAddOrSubtract("arg + 123", 123, IntIncrementNode.class);
    basicAddOrSubtract("var + 245", 245, IntIncrementNode.class);
    basicAddOrSubtract("field + 645", 645, IntIncrementNode.class);

    basicAddOrSubtract("arg - 123", -123, IntIncrementNode.class);
    basicAddOrSubtract("var - 245", -245, IntIncrementNode.class);
    basicAddOrSubtract("field - 645", -645, IntIncrementNode.class);
  }

  @Test
  public void testIfTrueAndIncArg() {
    SequenceNode seq = (SequenceNode) parseMethod(
        "test: arg = (\n"
            + "#start.\n"
            + "(self key: 5) ifTrue: [ arg + 1 ]. #end )");

    IfInlinedLiteralNode ifNode = (IfInlinedLiteralNode) read(seq, "expressions", 1);

    IntIncrementNode inc = read(ifNode, "bodyNode", IntIncrementNode.class);
    LocalArgumentReadNode arg = (LocalArgumentReadNode) inc.getRcvr();
    assertEquals(1, arg.argumentIndex);
    assertEquals("arg", arg.getInvocationIdentifier().getString());
  }

  @Test
  public void testFieldInc() {
    basicAddOrSubtract("field := field + 1", 1, IntUninitIncFieldNode.class);
    basicAddOrSubtract("field := field - 1", -1, IntUninitIncFieldNode.class);
    basicAddOrSubtract("field := field + 1123", 1123, IntUninitIncFieldNode.class);
    basicAddOrSubtract("field := field - 234234", -234234, IntUninitIncFieldNode.class);

  }

  private void fieldIncWithExpr(final String test, final Class<?> nodeType) {
    addField("field");
    SequenceNode seq = (SequenceNode) parseMethod(
        "test: arg = ( | var | \n" + test + " )");

    ExpressionNode testExpr = read(seq, "expressions", 0);
    assertThat(testExpr, instanceOf(nodeType));
  }

  @Test
  public void testFieldIncWithExpression() {
    fieldIncWithExpr("field := field + (23 + 434)", UninitIncFieldNode.class);
    fieldIncWithExpr("field := field + var", UninitIncFieldNode.class);
    fieldIncWithExpr("field := field + arg", UninitIncFieldNode.class);
  }

  @Test
  public void testLocalInc() {
    basicAddOrSubtract("var := var + 1", 1, LocalVariableIncNode.class);
    basicAddOrSubtract("var := var - 1", -1, LocalVariableIncNode.class);
    basicAddOrSubtract("var := var + 1123", 1123, LocalVariableIncNode.class);
    basicAddOrSubtract("var := var - 234234", -234234, LocalVariableIncNode.class);
  }

  private void inBlock(final String test, final long literalValue,
      final Class<?> nodeType) {
    addField("field");
    SequenceNode seq = (SequenceNode) parseMethod(
        "test: arg = ( | var | \n" + test + " )");

    BlockNode block = (BlockNode) read(seq, "expressions", 0);
    ExpressionNode testExpr =
        read(block.getMethod().getInvokable(), "expressionOrSequence", ExpressionNode.class);
    assertThat(testExpr, instanceOf(nodeType));
    long value = read(testExpr, "incValue", Long.class);
    assertEquals(literalValue, value);
  }

  @Test
  public void testNonLocalInc() {
    inBlock("[ var := var + 1 ]", 1, NonLocalVariableIncNode.class);
    inBlock("[ var := var - 1 ]", -1, NonLocalVariableIncNode.class);
    inBlock("[ var := var + 1123 ]", 1123, NonLocalVariableIncNode.class);
    inBlock("[ var := var - 234234 ]", -234234, NonLocalVariableIncNode.class);
  }

}
