package trufflesom.supernodes;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.SequenceNode;
import trufflesom.interpreter.nodes.supernodes.StringEqualsNode;
import trufflesom.tests.AstTestSetup;


public class StringEqualsTests extends AstTestSetup {

  @SuppressWarnings("unchecked")
  private <T> T assertThatMainNodeIs(final String test, final Class<T> expectedNode) {
    addField("field");
    SequenceNode seq = (SequenceNode) parseMethod(
        "test: arg = ( | var | \n" + test + " )");

    ExpressionNode testExpr = read(seq, "expressions", 0);
    assertThat(testExpr, instanceOf(expectedNode));
    return (T) testExpr;
  }

  @Test
  public void testStringEqual() {
    assertThatMainNodeIs("field = 'str'", StringEqualsNode.class);
    assertThatMainNodeIs("arg = 'str'", StringEqualsNode.class);
    assertThatMainNodeIs("var = 'str'", StringEqualsNode.class);
    assertThatMainNodeIs("('s' + 'dd') = 'str'", StringEqualsNode.class);

    assertThatMainNodeIs("'str' = field", StringEqualsNode.class);
    assertThatMainNodeIs("'str' = arg", StringEqualsNode.class);
    assertThatMainNodeIs("'str' = var", StringEqualsNode.class);
    assertThatMainNodeIs("'str' = ('s' + 'dd')", StringEqualsNode.class);
  }
}
