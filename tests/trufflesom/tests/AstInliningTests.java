package trufflesom.tests;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.truffle.api.nodes.Node;

import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgumentReadNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.NonLocalArgumentReadNode;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.FieldNode.FieldReadNode;
import trufflesom.interpreter.nodes.GlobalNode.FalseGlobalNode;
import trufflesom.interpreter.nodes.GlobalNode.NilGlobalNode;
import trufflesom.interpreter.nodes.GlobalNode.TrueGlobalNode;
import trufflesom.interpreter.nodes.GlobalNode.UninitializedGlobalReadNode;
import trufflesom.interpreter.nodes.LocalVariableNode.LocalVariableWriteNode;
import trufflesom.interpreter.nodes.NonLocalVariableNode.NonLocalVariableReadNode;
import trufflesom.interpreter.nodes.NonLocalVariableNode.NonLocalVariableWriteNode;
import trufflesom.interpreter.nodes.ReturnNonLocalNode.ReturnLocalNode;
import trufflesom.interpreter.nodes.SequenceNode;
import trufflesom.interpreter.nodes.literals.BlockNode;
import trufflesom.interpreter.nodes.literals.BlockNode.BlockNodeWithContext;
import trufflesom.interpreter.nodes.literals.DoubleLiteralNode;
import trufflesom.interpreter.nodes.literals.GenericLiteralNode;
import trufflesom.interpreter.nodes.literals.IntegerLiteralNode;
import trufflesom.interpreter.nodes.specialized.BooleanInlinedLiteralNode.AndInlinedLiteralNode;
import trufflesom.interpreter.nodes.specialized.BooleanInlinedLiteralNode.OrInlinedLiteralNode;
import trufflesom.interpreter.nodes.specialized.IfInlinedLiteralNode;
import trufflesom.interpreter.nodes.specialized.IfTrueIfFalseInlinedLiteralsNode.FalseIfElseLiteralNode;
import trufflesom.interpreter.nodes.specialized.IfTrueIfFalseInlinedLiteralsNode.TrueIfElseLiteralNode;
import trufflesom.interpreter.nodes.specialized.IntToDoInlinedLiteralsNode;
import trufflesom.interpreter.nodes.specialized.whileloops.WhileInlinedLiteralsNode;
import trufflesom.interpreter.supernodes.NonLocalVariableIncNode;
import trufflesom.interpreter.supernodes.UninitFieldIncNode;
import trufflesom.primitives.arithmetic.SubtractionPrim;
import trufflesom.primitives.arrays.DoPrim;


public class AstInliningTests extends AstTestSetup {

  private void accessArgFromInlinedBlock(final String argName, final int argIdx) {
    SequenceNode seq =
        (SequenceNode) parseMethod(
            "test: arg1 and: arg2 = ( true ifTrue: [ " + argName + " ] )");
    IfInlinedLiteralNode ifNode = (IfInlinedLiteralNode) read(seq, "expressions", 0);
    LocalArgumentReadNode arg = read(ifNode, "bodyNode", LocalArgumentReadNode.class);

    assertEquals(argName, arg.getInvocationIdentifier().getString());
    assertEquals(argIdx, arg.argumentIndex);
  }

  @Test
  public void testAccessArgFromInlinedBlock() {
    accessArgFromInlinedBlock("arg1", 1);
    accessArgFromInlinedBlock("arg2", 2);
  }

  @Test
  public void testAccessSelfFromInlinedBlock() {
    SequenceNode seq =
        (SequenceNode) parseMethod(
            "test: arg1 and: arg2 = ( true ifTrue: [ self ] )");
    IfInlinedLiteralNode ifNode = (IfInlinedLiteralNode) read(seq, "expressions", 0);
    LocalArgumentReadNode arg = read(ifNode, "bodyNode", LocalArgumentReadNode.class);

    assertTrue(arg.isSelfRead());
  }

  @Test
  public void testAccessBlockArgFromInlined() {
    SequenceNode seq = (SequenceNode) parseMethod(
        "test = ( [:arg |\n"
            + "  arg.\n"
            + "   true ifTrue: [ arg ] ] )");

    BlockNode blockNode = (BlockNode) read(seq, "expressions", 0);
    ExpressionNode[] blockExprs = getBlockExprs(blockNode);

    LocalArgumentReadNode argRead = (LocalArgumentReadNode) blockExprs[0];
    assertEquals(1, argRead.argumentIndex);
    assertEquals("arg", argRead.getInvocationIdentifier().getString());

    IfInlinedLiteralNode ifNode = (IfInlinedLiteralNode) blockExprs[1];
    LocalArgumentReadNode body = read(ifNode, "bodyNode", LocalArgumentReadNode.class);
    assertEquals("arg", body.getInvocationIdentifier().getString());
    assertEquals(1, body.argumentIndex);
  }

  private void literalTest(final String literalStr, final Class<?> cls) {
    SequenceNode seq = (SequenceNode) parseMethod(
        "test = ( self method ifTrue: [ " + literalStr + " ]. )");
    IfInlinedLiteralNode ifNode = (IfInlinedLiteralNode) read(seq, "expressions", 0);
    Node literal = read(ifNode, "bodyNode");
    assertThat(literal, instanceOf(cls));
  }

  @Test
  public void testIfTrueWithLiteralReturn() {
    literalTest("0", IntegerLiteralNode.class);
    literalTest("1", IntegerLiteralNode.class);
    literalTest("-10", IntegerLiteralNode.class);
    literalTest("3333", IntegerLiteralNode.class);
    literalTest("'str'", GenericLiteralNode.class);
    literalTest("#sym", GenericLiteralNode.class);
    literalTest("1.1", DoubleLiteralNode.class);
    literalTest("-2342.234", DoubleLiteralNode.class);

    literalTest("true", TrueGlobalNode.class);
    literalTest("false", FalseGlobalNode.class);
    literalTest("nil", NilGlobalNode.class);

    literalTest("SomeGlobal", UninitializedGlobalReadNode.class);
    literalTest("[]", BlockNode.class);
    literalTest("[ self ]", BlockNodeWithContext.class);

  }

  private void ifArg(final String ifSelector, final boolean expected) {
    SequenceNode seq = (SequenceNode) parseMethod(
        "test: arg = (\n"
            + "#start.\n"
            + "self method " + ifSelector + " [ arg ]. #end )");
    IfInlinedLiteralNode ifNode = (IfInlinedLiteralNode) read(seq, "expressions", 1);
    boolean actualExpectedBool = read(ifNode, "expectedBool", Boolean.class);
    assertEquals(expected, actualExpectedBool);
  }

  @Test
  public void testIfArg() {
    ifArg("ifTrue:", true);
    ifArg("ifFalse:", false);
  }

  @Test
  public void testIfTrueAndIncField() {
    addField("field");
    SequenceNode seq = (SequenceNode) parseMethod(
        "test: arg = (\n"
            + "#start.\n"
            + "(self key: 5) ifTrue: [ field := field + 1 ]. #end )");

    IfInlinedLiteralNode ifNode = (IfInlinedLiteralNode) read(seq, "expressions", 1);
    UninitFieldIncNode incNode = read(ifNode, "bodyNode", UninitFieldIncNode.class);

    int fieldIdx = read(incNode, "fieldIndex", Integer.class);
    assertEquals(0, fieldIdx);

    LocalArgumentReadNode selfNode = (LocalArgumentReadNode) incNode.getSelf();
    assertTrue(selfNode.isSelfRead());
  }

  @Test
  public void testNestedIf() {
    addField("field");
    SequenceNode seq = (SequenceNode) parseMethod(
        "test: arg = (\n"
            + "  true ifTrue: [\n"
            + "    false ifFalse: [\n"
            + "      ^ field - arg ] ] )");

    IfInlinedLiteralNode ifTrueNode = (IfInlinedLiteralNode) read(seq, "expressions", 0);
    IfInlinedLiteralNode ifFalseNode =
        read(ifTrueNode, "bodyNode", IfInlinedLiteralNode.class);
    ReturnLocalNode returnNode = read(ifFalseNode, "bodyNode", ReturnLocalNode.class);

    SubtractionPrim subMsg =
        read(returnNode, "expression", SubtractionPrim.class);

    FieldReadNode rcvr = (FieldReadNode) subMsg.getReceiver();
    LocalArgumentReadNode selfRead = (LocalArgumentReadNode) rcvr.getSelf();
    assertTrue(selfRead.isSelfRead());

    LocalArgumentReadNode argNode = (LocalArgumentReadNode) subMsg.getArgument();
    assertEquals("arg", argNode.getInvocationIdentifier().getString());
    assertEquals(1, argNode.argumentIndex);
  }

  @Test
  public void testNestedIfsAndLocals() {
    addField("field");
    SequenceNode seq = (SequenceNode) parseMethod(
        "test: arg = (\n"
            + "  | a b c d |\n"
            + "  a := b.\n"
            + "  true ifTrue: [\n"
            + "    | e f g |\n"
            + "    e := 2.\n"
            + "    c := 3.\n"
            + "    false ifFalse: [\n"
            + "      | h i j |\n"
            + "      h := 1.\n"
            + "      ^ i - j - f - g - d ] ] )");
    IfInlinedLiteralNode ifTrueNode = (IfInlinedLiteralNode) read(seq, "expressions", 1);

    IfInlinedLiteralNode ifFalseNode =
        (IfInlinedLiteralNode) read(read(ifTrueNode, "bodyNode"), "expressions", 2);

    ExpressionNode[] body =
        read(read(ifFalseNode, "bodyNode"), "expressions", ExpressionNode[].class);

    LocalVariableWriteNode write = (LocalVariableWriteNode) body[0];
    assertEquals("h", write.getInvocationIdentifier().getString());

    assertThat((Object) body[1], instanceOf(ReturnLocalNode.class));
  }

  @Test
  public void testNestedIfsAndNonInlinedBlocks() {
    addField("field");
    SequenceNode seq = (SequenceNode) parseMethod(
        "test: arg = (\n"
            + "  | a |\n"
            + "  a := 1.\n"
            + "  true ifTrue: [\n"
            + "    | e |\n"
            + "    e := 0.\n"
            + "    [ a := 1. a ].\n"
            + "    false ifFalse: [\n"
            + "      | h |\n"
            + "      h := 1.\n"
            + "      [ h + a + e ].\n"
            + "      ^ h ] ].\n"
            + "  [ a ]\n"
            + ")");
    IfInlinedLiteralNode ifTrueNode = (IfInlinedLiteralNode) read(seq, "expressions", 1);

    BlockNode blockNode =
        (BlockNode) read(read(ifTrueNode, "bodyNode"), "expressions", 1);
    NonLocalVariableWriteNode write = (NonLocalVariableWriteNode) read(
        read(blockNode.getMethod().getInvokable(), "expressionOrSequence"), "expressions", 0);
    assertEquals(1, write.getContextLevel());
    assertEquals("a", write.getInvocationIdentifier().getString());

    IfInlinedLiteralNode ifFalseNode =
        (IfInlinedLiteralNode) read(read(ifTrueNode, "bodyNode"), "expressions", 2);
    ExpressionNode[] body =
        read(read(ifFalseNode, "bodyNode"), "expressions", ExpressionNode[].class);

    LocalVariableWriteNode writeH = (LocalVariableWriteNode) body[0];
    assertEquals("h", writeH.getInvocationIdentifier().getString());

    assertThat((Object) body[2], instanceOf(ReturnLocalNode.class));
  }

  @Test
  public void testNestedNonInlinedBlocks() {
    addField("field");
    SequenceNode seq = (SequenceNode) parseMethod(
        "test: a = ( | b |\n"
            + " true ifFalse: [ | c |\n"
            + "   a. b. c.\n"
            + "   [:d |\n"
            + "      a. b. c. d.\n"
            + "      [:e |\n"
            + "        a. b. c. d. e ] ] ]\n"
            + ")");
    IfInlinedLiteralNode ifFalseNode = (IfInlinedLiteralNode) read(seq, "expressions", 0);

    BlockNode blockNode =
        (BlockNode) read(read(ifFalseNode, "bodyNode"), "expressions", 3);
    ExpressionNode[] blockExprs = getBlockExprs(blockNode);

    NonLocalArgumentReadNode readA = (NonLocalArgumentReadNode) blockExprs[0];
    assertEquals(1, readA.getContextLevel());
    assertEquals("a", readA.getInvocationIdentifier().getString());
    assertEquals(1, (int) read(readA, "argumentIndex", Integer.class));

    NonLocalVariableReadNode readB = (NonLocalVariableReadNode) blockExprs[1];
    assertEquals(1, readB.getContextLevel());
    assertEquals("b", readB.getInvocationIdentifier().getString());

    blockNode = (BlockNode) blockExprs[4];
    blockExprs = getBlockExprs(blockNode);
    readA = (NonLocalArgumentReadNode) blockExprs[0];
    assertEquals(2, readA.getContextLevel());
    assertEquals("a", readA.getInvocationIdentifier().getString());
    assertEquals(1, (int) read(readA, "argumentIndex", Integer.class));

    readB = (NonLocalVariableReadNode) blockExprs[1];
    assertEquals(2, readB.getContextLevel());
    assertEquals("b", readB.getInvocationIdentifier().getString());
  }

  private void ifTrueIfFalseReturn(final String sel1, final String sel2,
      final Class<?> cls) {
    SequenceNode seq = (SequenceNode) parseMethod(
        "test: arg1 with: arg2 = (\n"
            + "   #start.\n"
            + "   ^ self method " + sel1 + " [ ^ arg1 ] " + sel2 + " [ arg2 ]\n"
            + "   )");

    Node ifNode = read(seq, "expressions", 1);
    assertThat(ifNode, instanceOf(cls));
  }

  @Test
  public void testIfTrueIfFalseReturn() {
    ifTrueIfFalseReturn("ifTrue:", "ifFalse:", TrueIfElseLiteralNode.class);
    ifTrueIfFalseReturn("ifFalse:", "ifTrue:", FalseIfElseLiteralNode.class);
  }

  private void whileInlining(final String whileSel, final boolean expectedBool) {
    SequenceNode seq = (SequenceNode) parseMethod(
        "test: arg1 with: arg2 = (\n"
            + "  [ arg1 ] " + whileSel + " [ arg2 ]\n"
            + ")");

    WhileInlinedLiteralsNode whileNode =
        (WhileInlinedLiteralsNode) read(seq, "expressions", 0);
    assertEquals(expectedBool, read(whileNode, "expectedBool", Boolean.class));
  }

  @Test
  public void testWhileInlining() {
    whileInlining("whileTrue:", true);
    whileInlining("whileFalse:", false);
  }

  @Test
  public void testBlockBlockInlinedSelf() {
    addField("field");
    SequenceNode seq = (SequenceNode) parseMethod(
        "test = (\n"
            + "[:a |\n"
            + "  [:b |\n"
            + "     b ifTrue: [ field := field + 1 ] ] ]\n"
            + ")");

    BlockNode blockNodeA = (BlockNode) read(seq, "expressions", 0);

    BlockNode blockNodeB =
        (BlockNode) read(blockNodeA.getMethod().getInvokable(), "expressionOrSequence");

    IfInlinedLiteralNode blockBIfTrue =
        (IfInlinedLiteralNode) read(blockNodeB.getMethod().getInvokable(),
            "expressionOrSequence");

    LocalArgumentReadNode readB =
        read(blockBIfTrue, "conditionNode", LocalArgumentReadNode.class);
    assertEquals("b", readB.getInvocationIdentifier().getString());
    assertEquals(1, readB.argumentIndex);

    UninitFieldIncNode incNode = read(blockBIfTrue, "bodyNode", UninitFieldIncNode.class);
    NonLocalArgumentReadNode selfNode = (NonLocalArgumentReadNode) incNode.getSelf();
    assertEquals(2, selfNode.getContextLevel());
    assertEquals(0, (int) read(incNode, "fieldIndex", Integer.class));
  }

  @Test
  public void testToDoBlockBlockInlinedSelf() {
    addField("field");
    SequenceNode seq = (SequenceNode) parseMethod("test = (\n"
        + "| l1 l2 |\n"
        + "1 to: 2 do: [:a |\n"
        + "  l1 do: [:b |\n"
        + "    b ifTrue: [ l2 := l2 + 1 ] ] ]\n"
        + ")");

    IntToDoInlinedLiteralsNode toDo =
        (IntToDoInlinedLiteralsNode) read(seq, "expressions", 0);

    DoPrim doPrim = read(toDo, "body", DoPrim.class);
    BlockNode blockA = (BlockNode) doPrim.getArgument();
    IfInlinedLiteralNode blockBIfTrue =
        read(blockA.getMethod().getInvokable(), "expressionOrSequence",
            IfInlinedLiteralNode.class);

    LocalArgumentReadNode readNode =
        read(blockBIfTrue, "conditionNode", LocalArgumentReadNode.class);
    assertEquals("b", readNode.getInvocationIdentifier().getString());
    assertEquals(1, readNode.argumentIndex);

    NonLocalVariableIncNode writeNode =
        read(blockBIfTrue, "bodyNode", NonLocalVariableIncNode.class);
    assertEquals(1, writeNode.getContextLevel());
    assertEquals("l2", writeNode.getInvocationIdentifier().getString());
  }

  @Test
  public void testFieldReadInlining() {
    addField("field");
    SequenceNode seq = (SequenceNode) parseMethod("test = ( true and: [ field ] )");

    AndInlinedLiteralNode and =
        (AndInlinedLiteralNode) read(seq, "expressions", 0);
    assertThat(read(and, "argumentNode"), instanceOf(FieldReadNode.class));
  }

  private Object inliningOf(final String selector) {
    SequenceNode seq = (SequenceNode) parseMethod("test = ( true " + selector + " [ #val ] )");
    return read(seq, "expressions", 0);
  }

  @Test
  public void testInliningOf() {
    assertThat(inliningOf("or:"), instanceOf(OrInlinedLiteralNode.class));
    assertThat(inliningOf("||"), instanceOf(OrInlinedLiteralNode.class));
    assertThat(inliningOf("and:"), instanceOf(AndInlinedLiteralNode.class));
    assertThat(inliningOf("&&"), instanceOf(AndInlinedLiteralNode.class));
  }

  @Test
  public void testInliningOfToDo() {
    SequenceNode seq = (SequenceNode) parseMethod("test = ( 1 to: 2 do: [:i | i ] )");
    IntToDoInlinedLiteralsNode toDo =
        (IntToDoInlinedLiteralsNode) read(seq, "expressions", 0);
    assertEquals("i", toDo.getIndexName());
  }
}
