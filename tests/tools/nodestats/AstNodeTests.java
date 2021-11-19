package tools.nodestats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.List;

import org.junit.Test;


public class AstNodeTests {

  @Test
  public void testCloneHeight0() {
    AstNode tree = new AstNode(getClass());
    tree.collectTreesAndDetermineHeight(100, null);

    AstNode clone = tree.cloneWithMaxHeight(0);
    assertSame(tree, clone);

    tree.addChild(new AstNode(getClass()));

    tree.collectTreesAndDetermineHeight(100, null);

    clone = tree.cloneWithMaxHeight(0);
    assertNotSame(tree, clone);

    assertFalse(tree.equals(clone));
    assertNotEquals(tree.hashCode(), clone.hashCode());
  }

  @Test
  public void testCloneHeight1() {
    AstNode tree = new AstNode(getClass());
    tree.collectTreesAndDetermineHeight(100, null);

    AstNode clone = tree.cloneWithMaxHeight(1);
    assertSame(tree, clone);

    tree.addChild(new AstNode(getClass()));

    tree.collectTreesAndDetermineHeight(100, null);

    clone = tree.cloneWithMaxHeight(1);
    assertSame(tree, clone);
  }

  @Test
  public void testCloneHeight2() {
    AstNode tree = new AstNode(getClass());

    AstNode child1 = new AstNode(getClass());
    child1.addChild(new AstNode(getClass()));
    child1.addChild(new AstNode(getClass()));

    AstNode child2 = new AstNode(getClass());
    AstNode child21 = new AstNode(getClass());
    child2.addChild(child21);
    child2.addChild(new AstNode(getClass()));

    child21.addChild(new AstNode(getClass()));

    tree.addChild(child1);
    tree.addChild(child2);

    tree.collectTreesAndDetermineHeight(100, null);

    AstNode clone = tree.cloneWithMaxHeight(1);
    assertNotSame(tree, clone);
    List<AstNode> children = clone.getChildren();

    assertEquals(2, children.size());

    AstNode c1 = children.get(0);
    AstNode c2 = children.get(1);

    assertNull(c1.getChildren());
    assertNull(c2.getChildren());
  }
}
