package trufflesom.supernodes;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.SequenceNode;
import trufflesom.interpreter.supernodes.LocalArgGreaterThanInt;
import trufflesom.primitives.arithmetic.GreaterThanPrim;
import trufflesom.tests.AstTestSetup;


public class GreaterThanTests extends AstTestSetup {
  @SuppressWarnings("unchecked")
  private <T> T assertThatMainNodeIs(final String test, final Class<T> expectedNode) {
    SequenceNode seq = (SequenceNode) parseMethod(
        "test: arg = ( | l1 l2 l3 l4 | \n" + test + " )");

    ExpressionNode testExpr = read(seq, "expressions", 0);
    assertThat(testExpr, instanceOf(expectedNode));
    return (T) testExpr;
  }

  @Test
  public void testBasics() {
    assertThatMainNodeIs("arg > 0", LocalArgGreaterThanInt.class);

    // unsupported at the moment
    assertThatMainNodeIs("l1 > 0", GreaterThanPrim.class);
    assertThatMainNodeIs("3 > 0", GreaterThanPrim.class);
    assertThatMainNodeIs("3 > 0", GreaterThanPrim.class);
  }
}
