package trufflesom.tools.nodestats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;


public class AstNodeTests {

  @Test
  public void testCloneHeight0() {
    AstNode tree = new AstNode(getClass(), null);
    tree.collectTreesAndDetermineHeight(100, null);

    AstNode clone = tree.cloneWithMaxHeight(0);
    assertNotSame(tree, clone);
    assertEquals(tree, clone);

    tree.addChild(new AstNode(getClass(), null));

    tree.collectTreesAndDetermineHeight(100, null);

    clone = tree.cloneWithMaxHeight(0);
    assertNotSame(tree, clone);

    assertFalse(tree.equals(clone));
    assertNotEquals(tree.hashCode(), clone.hashCode());
  }

  @Test
  public void testCloneHeight1() {
    AstNode tree = new AstNode(getClass(), null);
    tree.collectTreesAndDetermineHeight(100, null);

    AstNode clone = tree.cloneWithMaxHeight(1);
    assertNotSame(tree, clone);
    assertEquals(tree, clone);

    tree.addChild(new AstNode(getClass(), null));

    tree.collectTreesAndDetermineHeight(100, null);

    clone = tree.cloneWithMaxHeight(1);
    assertNotSame(tree, clone);
    assertEquals(tree, clone);
  }

  @Test
  public void testCloneHeight2() {
    AstNode tree = new AstNode(getClass(), null);

    AstNode child1 = new AstNode(getClass(), null);
    child1.addChild(new AstNode(getClass(), null));
    child1.addChild(new AstNode(getClass(), null));

    AstNode child2 = new AstNode(getClass(), null);
    AstNode child21 = new AstNode(getClass(), null);
    child2.addChild(child21);
    child2.addChild(new AstNode(getClass(), null));

    child21.addChild(new AstNode(getClass(), null));

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
