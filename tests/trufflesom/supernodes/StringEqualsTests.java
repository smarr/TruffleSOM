package trufflesom.supernodes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import org.junit.Test;

import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.SequenceNode;
import trufflesom.interpreter.nodes.literals.BlockNode;
import trufflesom.interpreter.supernodes.LocalFieldStringEqualsNode;
import trufflesom.interpreter.supernodes.NonLocalFieldStringEqualsNode;
import trufflesom.interpreter.supernodes.StringEqualsNode;
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

  @SuppressWarnings("unchecked")
  private <T> T assertInBlock(final String test, final Class<T> expectedNode) {
    addField("field");
    SequenceNode seq = (SequenceNode) parseMethod(
        "test: arg = ( | var | \n" + test + " )");

    BlockNode block = (BlockNode) read(seq, "expressions", 0);
    ExpressionNode testExpr =
        read(block.getMethod().getInvokable(), "body", ExpressionNode.class);
    assertThat(testExpr, instanceOf(expectedNode));
    return (T) testExpr;
  }

  @Test
  public void testStringEqual() {
    assertThatMainNodeIs("field = 'str'", LocalFieldStringEqualsNode.class);
    assertThatMainNodeIs("arg = 'str'", StringEqualsNode.class);
    assertThatMainNodeIs("var = 'str'", StringEqualsNode.class);
    assertThatMainNodeIs("('s' + 'dd') = 'str'", StringEqualsNode.class);

    assertThatMainNodeIs("'str' = field", LocalFieldStringEqualsNode.class);
    assertThatMainNodeIs("'str' = arg", StringEqualsNode.class);
    assertThatMainNodeIs("'str' = var", StringEqualsNode.class);
    assertThatMainNodeIs("'str' = ('s' + 'dd')", StringEqualsNode.class);
  }

  @Test
  public void testStringEqualInBlock() {
    assertInBlock("[ field = 'str' ] ", NonLocalFieldStringEqualsNode.class);
    assertInBlock("[ arg = 'str' ]", StringEqualsNode.class);
    assertInBlock("[ var = 'str' ]", StringEqualsNode.class);

    assertInBlock("[:a | a = 'str' ]", StringEqualsNode.class);
    assertInBlock("[ | v | v = 'str' ]", StringEqualsNode.class);

    assertInBlock("[ ('s' + 'dd') = 'str' ]", StringEqualsNode.class);

    assertInBlock("[ 'str' = field ]", NonLocalFieldStringEqualsNode.class);
    assertInBlock("[ 'str' = arg ]", StringEqualsNode.class);
    assertInBlock("[ 'str' = var ]", StringEqualsNode.class);
    assertInBlock("[:a | 'str' = a ]", StringEqualsNode.class);
    assertInBlock("[ | v|  'str' = v ]", StringEqualsNode.class);
    assertInBlock("[ 'str' = ('s' + 'dd') ]", StringEqualsNode.class);
  }
}
