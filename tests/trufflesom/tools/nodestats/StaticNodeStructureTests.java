package trufflesom.tools.nodestats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;

import trufflesom.interpreter.LexicalScope;
import trufflesom.interpreter.Method;
import trufflesom.interpreter.MethodNoCatch;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.literals.LiteralNode;
import trufflesom.primitives.arithmetic.AdditionPrim;
import trufflesom.primitives.arithmetic.AdditionPrimFactory;
import trufflesom.primitives.arithmetic.SubtractionPrimFactory;
import trufflesom.primitives.basics.AsStringPrimFactory;
import trufflesom.primitives.basics.IntegerPrimsFactory.AbsPrimFactory;
import trufflesom.primitives.basics.IntegerPrimsFactory.AsDoubleValueFactory;
import trufflesom.tests.TruffleTestSetup;


public class StaticNodeStructureTests extends TruffleTestSetup {

  @Test
  public void testSimpleAdd() {
    ExpressionNode body =
        AdditionPrimFactory.create(LiteralNode.create(1L), LiteralNode.create(2L));
    Method m = constructMethod(body);

    NodeStatisticsCollector s = new NodeStatisticsCollector(5, new HashMap<>());
    s.add(m);

    s.collectStats();

    Set<SubTree> cs = s.getSubTreesWithOccurrenceScore();

    assertEquals(3, cs.size());
    Iterator<SubTree> i = cs.iterator();
    SubTree c1 = i.next();
    SubTree c2 = i.next();
    SubTree c3 = i.next();

    assertTrue(c1.getRoot().getNodeClass() == MethodNoCatch.class
        || (Class<?>) c2.getRoot().getNodeClass() == MethodNoCatch.class
        || (Class<?>) c3.getRoot().getNodeClass() == MethodNoCatch.class);

    assertTrue(AdditionPrim.class.isAssignableFrom(c1.getRoot().getNodeClass()) ||
        AdditionPrim.class.isAssignableFrom(c2.getRoot().getNodeClass()) ||
        AdditionPrim.class.isAssignableFrom(c3.getRoot().getNodeClass()));
  }

  @Test
  public void testFindAllExpectedUniquieSubTreesNoDuplicates() {
    ExpressionNode body = AdditionPrimFactory.create(
        AbsPrimFactory.create(
            LiteralNode.create(1L)),
        SubtractionPrimFactory.create(
            AsDoubleValueFactory.create(
                LiteralNode.create(44L)),
            AsStringPrimFactory.create(
                LiteralNode.create(4445.55d))));
    Method m = constructMethod(body);

    NodeStatisticsCollector s = new NodeStatisticsCollector(5, new HashMap<>());
    s.add(m);

    s.collectStats();

    Set<SubTree> cs = s.getSubTreesWithOccurrenceScore();

    assertEquals(12, cs.size());
  }

  @Test
  public void testFindAllExpectedUniquieSubTreesWithDuplicates() {
    ExpressionNode body = AdditionPrimFactory.create(
        AbsPrimFactory.create(
            LiteralNode.create(1L)),
        AdditionPrimFactory.create(
            AbsPrimFactory.create(
                LiteralNode.create(44L)),
            AdditionPrimFactory.create(
                AbsPrimFactory.create(
                    LiteralNode.create(44L)),
                AbsPrimFactory.create(
                    LiteralNode.create(4445.55d)))));
    Method m = constructMethod(body);

    NodeStatisticsCollector s = new NodeStatisticsCollector(5, new HashMap<>());
    s.add(m);

    s.collectStats();

    Set<SubTree> cs = s.getSubTreesWithOccurrenceScore();

    assertEquals(15, cs.size());
  }

  private static Method constructMethod(final ExpressionNode body) {
    LexicalScope scope = new LexicalScope(null);
    scope.finalizeVariables(0);
    return new MethodNoCatch("test", null, 0, body, scope, body);
  }
}
