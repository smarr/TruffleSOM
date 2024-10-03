package trufflesom.supernodes;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.LocalVariableNode.LocalVariableWriteNode;
import trufflesom.interpreter.nodes.NonLocalVariableNode.NonLocalVariableWriteNode;
import trufflesom.interpreter.nodes.SequenceNode;
import trufflesom.interpreter.nodes.literals.BlockNode;
import trufflesom.interpreter.supernodes.LocalVariableReadSquareWriteNode;
import trufflesom.interpreter.supernodes.LocalVariableSquareNode;
import trufflesom.interpreter.supernodes.NonLocalVariableReadSquareWriteNode;
import trufflesom.interpreter.supernodes.NonLocalVariableSquareNode;
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
    assertEquals(s.getLocal().name, "l2");

    s = assertThatMainNodeIs("l1 * l1.", LocalVariableSquareNode.class);
    assertEquals(s.getLocal().name, "l1");

    assertThatMainNodeIs("l1 * l3.", MultiplicationPrim.class);
  }

  @SuppressWarnings("unchecked")
  private <T> T inBlock(final String test, final Class<T> expectedNode) {
    addField("field");
    SequenceNode seq = (SequenceNode) parseMethod(
        "test: arg = ( | l1 l2 l3 l4 | \n" + test + " )");

    BlockNode block = (BlockNode) read(seq, "expressions", 0);
    ExpressionNode testExpr =
        read(block.getMethod().getInvokable(), "body", ExpressionNode.class);
    assertThat(testExpr, instanceOf(expectedNode));
    return (T) testExpr;
  }

  @Test
  public void testJustSquareNonLocals() {
    NonLocalVariableSquareNode s = inBlock("[ l2 * l2 ]", NonLocalVariableSquareNode.class);
    assertEquals(s.getLocal().name, "l2");

    s = inBlock("[ l1 * l1 ]", NonLocalVariableSquareNode.class);
    assertEquals(s.getLocal().name, "l1");

    inBlock("[ l1 * l3 ]", MultiplicationPrim.class);
  }

  @Test
  public void testSquareAndAssignLocal() {
    assertThatMainNodeIs("l1 := l2 * l2.", LocalVariableReadSquareWriteNode.class);
    assertThatMainNodeIs("l2 := l2 * l2.", LocalVariableReadSquareWriteNode.class);

    assertThatMainNodeIs("l3 := l1 * l2.", LocalVariableWriteNode.class);
  }

  @Test
  public void testSquareAndAssignNonLocal() {
    inBlock("[ l1 := l2 * l2 ]", NonLocalVariableReadSquareWriteNode.class);
    inBlock("[ l2 := l2 * l2 ]", NonLocalVariableReadSquareWriteNode.class);
    inBlock("[|bl1| l2 := bl1 * bl1 ]", NonLocalVariableReadSquareWriteNode.class);
    inBlock("[|bl1| bl1 := l2 * l2 ]", NonLocalVariableReadSquareWriteNode.class);

    inBlock("[ l3 := l1 * l2 ]", NonLocalVariableWriteNode.class);
  }
}
