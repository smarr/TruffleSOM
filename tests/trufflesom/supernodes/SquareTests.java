package trufflesom.supernodes;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.LocalVariableNode.LocalVariableWriteNode;
import trufflesom.interpreter.nodes.SequenceNode;
import trufflesom.interpreter.nodes.supernodes.AssignLocalSquareToLocalNode;
import trufflesom.primitives.arithmetic.MultiplicationPrim;
import trufflesom.tests.AstTestSetup;


public class SquareTests extends AstTestSetup {
  private ExpressionNode assertThatMainNodeIs(final String test, final Class<?> expectedNode) {
    SequenceNode seq = (SequenceNode) parseMethod(
        "test = ( | l1 l2 l3 l4 | \n" + test + " )");

    ExpressionNode testExpr = read(seq, "expressions", 0);
    assertThat(testExpr, instanceOf(expectedNode));
    return testExpr;
  }

  @Test
  public void testJustSquareLocals() {
    assertThatMainNodeIs("l2 * l2.", AssignLocalSquareToLocalNode.class);
    assertThatMainNodeIs("l1 * l1.", AssignLocalSquareToLocalNode.class);

    assertThatMainNodeIs("l1 * l3.", MultiplicationPrim.class);
  }

  @Test
  public void testJustSquareNoneLocals() {
    assertThatMainNodeIs("[ l2 * l2 ]", AssignLocalSquareToLocalNode.class);
    assertThatMainNodeIs("[ l1 * l1 ]", AssignLocalSquareToLocalNode.class);

    assertThatMainNodeIs("[ l1 * l3 ]", MultiplicationPrim.class);
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
