package trufflesom.supernodes;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.LocalVariableNode.LocalVariableWriteNode;
import trufflesom.interpreter.nodes.SequenceNode;
import trufflesom.interpreter.nodes.literals.BlockNode;
import trufflesom.interpreter.nodes.supernodes.AssignLocalSquareToLocalNode;
import trufflesom.interpreter.nodes.supernodes.LocalVariableSquareNode;
import trufflesom.interpreter.nodes.supernodes.NonLocalVariableSquareNode;
import trufflesom.primitives.arithmetic.MultiplicationPrim;
import trufflesom.tests.AstTestSetup;


public class SquareTests extends AstTestSetup {
  @SuppressWarnings("unchecked")
  private <T> T assertThatMainNodeIs(final String test, final Class<T> expectedNode) {
    SequenceNode seq = (SequenceNode) parseMethod(
        "test = ( | l1 l2 l3 l4 | \n" + test + " )");

    ExpressionNode testExpr = read(seq, "expressions", 0);
    assertThat(testExpr, instanceOf(expectedNode));
    return (T) testExpr;
  }

  @Test
  public void testJustSquareLocals() {
    LocalVariableSquareNode s =
        assertThatMainNodeIs("l2 * l2.", LocalVariableSquareNode.class);
    assertEquals(s.getLocal().name.getString(), "l2");

    s = assertThatMainNodeIs("l1 * l1.", LocalVariableSquareNode.class);
    assertEquals(s.getLocal().name.getString(), "l1");

    assertThatMainNodeIs("l1 * l3.", MultiplicationPrim.class);
  }

  @SuppressWarnings("unchecked")
  private <T> T inBlock(final String test, final Class<T> expectedNode) {
    addField("field");
    SequenceNode seq = (SequenceNode) parseMethod(
        "test: arg = ( | l1 l2 l3 l4 | \n" + test + " )");

    BlockNode block = (BlockNode) read(seq, "expressions", 0);
    ExpressionNode testExpr =
        read(block.getMethod().getInvokable(), "expressionOrSequence", ExpressionNode.class);
    assertThat(testExpr, instanceOf(expectedNode));
    return (T) testExpr;
  }

  @Test
  public void testJustSquareNonLocals() {
    NonLocalVariableSquareNode s = inBlock("[ l2 * l2 ]", NonLocalVariableSquareNode.class);
    assertEquals(s.getLocal().name.getString(), "l2");

    s = inBlock("[ l1 * l1 ]", NonLocalVariableSquareNode.class);
    assertEquals(s.getLocal().name.getString(), "l1");

    inBlock("[ l1 * l3 ]", MultiplicationPrim.class);
  }

  @Test
  public void testSquareAndAssignLocal() {
    assertThatMainNodeIs("l1 := l2 * l2.", AssignLocalSquareToLocalNode.class);
    assertThatMainNodeIs("l2 := l2 * l2.", AssignLocalSquareToLocalNode.class);

    assertThatMainNodeIs("l3 := l1 * l2.", LocalVariableWriteNode.class);
  }

  @Test
  public void testSquareAndAssignNonLocal() {
    assertThatMainNodeIs("[ l1 := l2 * l2 ]", AssignLocalSquareToLocalNode.class);
    assertThatMainNodeIs("[ l2 := l2 * l2 ]", AssignLocalSquareToLocalNode.class);

    assertThatMainNodeIs("[ l3 := l1 * l2 ]", LocalVariableWriteNode.class);
  }

}
