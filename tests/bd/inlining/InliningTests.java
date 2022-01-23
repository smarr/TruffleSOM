package bd.inlining;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import bd.basic.ProgramDefinitionError;
import bd.source.SourceCoordinate;
import bd.testsetup.AddNodeFactory;
import bd.testsetup.ExprNode;
import bd.testsetup.LambdaNode;
import bd.testsetup.StringId;
import bd.testsetup.ValueNode;
import bd.testsetup.ValueSpecializedNode;


public class InliningTests {
  private final long coord = SourceCoordinate.create(0, 10);

  private final InlinableNodes<String> nodes =
      new InlinableNodes<String>(new StringId(), Nodes.getInlinableNodes(),
          Nodes.getInlinableFactories());

  @Test
  public void testNonInlinableNode() throws ProgramDefinitionError {
    ExprNode[] argNodes = new ExprNode[] {AddNodeFactory.create(null, null)};
    assertNull(nodes.inline("value", argNodes, null, coord));
  }

  @Test
  public void testValueNode() throws ProgramDefinitionError {
    LambdaNode arg = new LambdaNode();
    ExprNode[] argNodes = new ExprNode[] {arg};

    ExprNode valueNode = nodes.inline("value", argNodes, null, coord);
    assertNotNull(valueNode);
    assertTrue(valueNode instanceof ValueNode);

    ValueNode value = (ValueNode) valueNode;

    assertNotEquals(arg, value.inlined);
    assertEquals(arg, value.original);

    assertTrue(value.trueVal);
    assertFalse(value.falseVal);

    assertTrue(valueNode.getSourceCoordinate() == coord);
  }

  @Test
  public void testValueSpecNode() throws ProgramDefinitionError {
    LambdaNode arg = new LambdaNode();
    ExprNode[] argNodes = new ExprNode[] {arg};

    ExprNode valueNode = nodes.inline("valueSpec", argNodes, null, coord);
    assertNotNull(valueNode);
    assertTrue(valueNode instanceof ValueSpecializedNode);

    ValueSpecializedNode value = (ValueSpecializedNode) valueNode;

    assertNotEquals(arg, value.inlined);
    assertEquals(arg, value.getLambda());
    assertTrue(valueNode.getSourceCoordinate() == coord);
  }

  @Test
  public void testEmptyInit() throws ProgramDefinitionError {
    InlinableNodes<String> n =
        new InlinableNodes<>(new StringId(), null, null);

    assertNull(n.inline("nonExisting", null, null, coord));
  }
}
