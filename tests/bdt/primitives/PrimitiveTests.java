package bdt.primitives;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.Test;

import com.oracle.truffle.api.nodes.Node;

import bdt.testsetup.AbsNode;
import bdt.testsetup.AddAbsNode;
import bdt.testsetup.AddNodeFactory;
import bdt.testsetup.ExprNode;


public class PrimitiveTests {

  private final Primitives ps = new Primitives();

  @Test
  public void testPrimitiveAnnotation() {
    Primitive[] annotations = Primitives.getPrimitiveAnnotation(AddNodeFactory.getInstance());
    Primitive p = annotations[0];

    assertEquals("Int", p.className());
    assertEquals("+", p.primitive());
    assertEquals(1, annotations.length);
  }

  @Test
  public void testEagerSpecializer() {
    Specializer<ExprNode, String> s = ps.getEagerSpecializer("+", null, null);
    assertNotNull(s);

    assertEquals("AddNodeFactory", s.getName());

    s = ps.getEagerSpecializer("---", null, null);
    assertNull(s);
  }

  @Test
  public void testParserSpecializer() {
    Specializer<ExprNode, String> s = ps.getParserSpecializer("+", null);
    assertNotNull(s);

    assertEquals("AddNodeFactory", s.getName());

    s = ps.getParserSpecializer("---", null);
    assertNull(s);
  }

  @Test
  public void testEagerSpecializerWithCustomSpecializer() {
    Specializer<ExprNode, String> s = ps.getEagerSpecializer("++", null, null);
    assertNotNull(s);

    assertEquals("AddWithSpecializerNodeFactory", s.getName());

    s = ps.getEagerSpecializer("---", null, null);
    assertNull(s);
  }

  @Test
  public void testParserSpecializerWithCustomSpecializer() {
    Specializer<ExprNode, String> s = ps.getParserSpecializer("++", null);
    assertNotNull(s);

    assertEquals("AddWithSpecializerNodeFactory", s.getName());

    s = ps.getParserSpecializer("---", null);
    assertNull(s);
  }

  @Test
  public void testExtraChild() {
    Specializer<ExprNode, String> s = ps.getParserSpecializer("addAbs", null);
    ExprNode n = s.create(null, new ExprNode[1], 0);
    assertTrue(n instanceof AddAbsNode);

    // Note: this is fragile, because it depends on the TruffleDSL node implementation strategy
    Iterator<Node> children = n.getChildren().iterator();
    assertTrue(children.next() instanceof AbsNode);
  }

  @Test
  public void testMatchForAddNode() {
    Specializer<ExprNode, String> s = ps.getEagerSpecializer("+", null, null);
    assertTrue("Match unconditionally, because `null` for args",
        s.matches(null, new ExprNode[2]));
    assertFalse("Don't match, because double as arg",
        s.matches(new Object[] {0.55}, new ExprNode[2]));
    assertTrue("Match, because int as arg",
        s.matches(new Object[] {42}, new ExprNode[2]));
  }
}
