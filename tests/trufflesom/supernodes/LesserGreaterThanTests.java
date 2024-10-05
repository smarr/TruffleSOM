package trufflesom.supernodes;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.SequenceNode;
import trufflesom.interpreter.supernodes.compare.GreaterThanIntNode;
import trufflesom.interpreter.supernodes.compare.LessThanIntNode;
import trufflesom.interpreter.supernodes.compare.LocalArgGreaterThanInt;
import trufflesom.interpreter.supernodes.compare.LocalArgLessThanInt;
import trufflesom.tests.AstTestSetup;


public class LesserGreaterThanTests extends AstTestSetup {
  @SuppressWarnings("unchecked")
  private <T> T assertThatMainNodeIs(final String test, final Class<T> expectedNode) {
    SequenceNode seq = (SequenceNode) parseMethod(
        "test: arg = ( | l1 l2 l3 l4 | \n" + test + " )");

    ExpressionNode testExpr = read(seq, "expressions", 0);
    assertThat(testExpr, instanceOf(expectedNode));
    return (T) testExpr;
  }

  @Test
  public void testGreaterThan() {
    assertThatMainNodeIs("(1 + 3) > 0", GreaterThanIntNode.class);

    assertThatMainNodeIs("l1 > 0", GreaterThanIntNode.class);
    assertThatMainNodeIs("3 > 0", GreaterThanIntNode.class);
    assertThatMainNodeIs("3 > 0", GreaterThanIntNode.class);

    assertThatMainNodeIs("arg > 0", LocalArgGreaterThanInt.class);
  }

  @Test
  public void testLesserThan() {
    assertThatMainNodeIs("(1 + 3) < 0", LessThanIntNode.class);

    assertThatMainNodeIs("l1 < 0", LessThanIntNode.class);
    assertThatMainNodeIs("3 < 0", LessThanIntNode.class);
    assertThatMainNodeIs("3 < 0", LessThanIntNode.class);

    assertThatMainNodeIs("arg < 0", LocalArgLessThanInt.class);
  }
}
