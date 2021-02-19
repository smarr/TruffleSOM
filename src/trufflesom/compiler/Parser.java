/**
 * Copyright (c) 2013 Stefan Marr,   stefan.marr@vub.ac.be
 * Copyright (c) 2009 Michael Haupt, michael.haupt@hpi.uni-potsdam.de
 * Software Architecture Group, Hasso Plattner Institute, Potsdam, Germany
 * http://www.hpi.uni-potsdam.de/swa/
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package trufflesom.compiler;

import static trufflesom.compiler.Symbol.And;
import static trufflesom.compiler.Symbol.Assign;
import static trufflesom.compiler.Symbol.At;
import static trufflesom.compiler.Symbol.Colon;
import static trufflesom.compiler.Symbol.Comma;
import static trufflesom.compiler.Symbol.Div;
import static trufflesom.compiler.Symbol.Double;
import static trufflesom.compiler.Symbol.EndBlock;
import static trufflesom.compiler.Symbol.EndTerm;
import static trufflesom.compiler.Symbol.Equal;
import static trufflesom.compiler.Symbol.Exit;
import static trufflesom.compiler.Symbol.Identifier;
import static trufflesom.compiler.Symbol.Integer;
import static trufflesom.compiler.Symbol.Keyword;
import static trufflesom.compiler.Symbol.KeywordSequence;
import static trufflesom.compiler.Symbol.Less;
import static trufflesom.compiler.Symbol.Minus;
import static trufflesom.compiler.Symbol.Mod;
import static trufflesom.compiler.Symbol.More;
import static trufflesom.compiler.Symbol.NONE;
import static trufflesom.compiler.Symbol.NewBlock;
import static trufflesom.compiler.Symbol.NewTerm;
import static trufflesom.compiler.Symbol.Not;
import static trufflesom.compiler.Symbol.OperatorSequence;
import static trufflesom.compiler.Symbol.Or;
import static trufflesom.compiler.Symbol.Per;
import static trufflesom.compiler.Symbol.Period;
import static trufflesom.compiler.Symbol.Plus;
import static trufflesom.compiler.Symbol.Pound;
import static trufflesom.compiler.Symbol.Primitive;
import static trufflesom.compiler.Symbol.STString;
import static trufflesom.compiler.Symbol.Separator;
import static trufflesom.compiler.Symbol.Star;
import static trufflesom.interpreter.SNodeFactory.createGlobalRead;
import static trufflesom.interpreter.SNodeFactory.createMessageSend;
import static trufflesom.interpreter.SNodeFactory.createSequence;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import bd.basic.ProgramDefinitionError;
import bd.inlining.InlinableNodes;
import bd.source.SourceCoordinate;
import bd.tools.structure.StructuralProbe;
import trufflesom.compiler.Lexer.Peek;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.FieldNode.FieldReadNode;
import trufflesom.interpreter.nodes.FieldNode.FieldWriteNode;
import trufflesom.interpreter.nodes.literals.ArrayLiteralNode;
import trufflesom.interpreter.nodes.literals.BigIntegerLiteralNode;
import trufflesom.interpreter.nodes.literals.BlockNode;
import trufflesom.interpreter.nodes.literals.BlockNode.BlockNodeWithContext;
import trufflesom.interpreter.nodes.literals.DoubleLiteralNode;
import trufflesom.interpreter.nodes.literals.IntegerLiteralNode;
import trufflesom.interpreter.nodes.literals.LiteralNode;
import trufflesom.interpreter.nodes.literals.StringLiteralNode;
import trufflesom.interpreter.nodes.literals.SymbolLiteralNode;
import trufflesom.primitives.Primitives;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SSymbol;


public class Parser {

  protected final Universe universe;
  private final Lexer      lexer;
  private final Source     source;

  private final InlinableNodes<SSymbol> inlinableNodes;

  protected final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe;

  private Symbol sym;
  private String text;
  private Symbol nextSym;

  private SourceCoordinate lastCoordinate;
  private SourceSection    lastMethodsSourceSection;
  private SourceSection    lastFullMethodsSourceSection;

  private static final List<Symbol> singleOpSyms        = new ArrayList<Symbol>();
  private static final List<Symbol> binaryOpSyms        = new ArrayList<Symbol>();
  private static final List<Symbol> keywordSelectorSyms = new ArrayList<Symbol>();

  static {
    for (Symbol s : new Symbol[] {Not, And, Or, Star, Div, Mod, Plus, Equal,
        More, Less, Comma, At, Per, NONE}) {
      singleOpSyms.add(s);
    }
    for (Symbol s : new Symbol[] {Or, Comma, Minus, Equal, Not, And, Or, Star,
        Div, Mod, Plus, Equal, More, Less, Comma, At, Per, NONE}) {
      binaryOpSyms.add(s);
    }
    for (Symbol s : new Symbol[] {Keyword, KeywordSequence}) {
      keywordSelectorSyms.add(s);
    }
  }

  @Override
  public String toString() {
    String name = source.getName();
    String coord = getCoordinate().toString();
    return "Parser(" + name + ", " + coord + ")";
  }

  public static class ParseError extends ProgramDefinitionError {
    private static final long      serialVersionUID = 425390202979033628L;
    private final SourceCoordinate sourceCoordinate;
    private final String           text;
    private final String           rawBuffer;
    private final String           fileName;
    private final Symbol           expected;
    private final Symbol           found;

    ParseError(final String message, final Symbol expected, final Parser parser) {
      super(message);
      this.sourceCoordinate = parser.getCoordinate();
      this.text = parser.text;
      this.rawBuffer = parser.lexer.getCurrentLine();
      this.fileName = parser.source.getName();
      this.expected = expected;
      this.found = parser.sym;
    }

    protected String expectedSymbolAsString() {
      return expected.toString();
    }

    @Override
    public String toString() {
      String msg = "%(file)s:%(line)d:%(column)d: error: " + getMessage();
      String foundStr;
      if (Parser.printableSymbol(found)) {
        foundStr = found + " (" + text + ")";
      } else {
        foundStr = found.toString();
      }
      msg += ": " + rawBuffer;
      String expectedStr = expectedSymbolAsString();

      msg = msg.replace("%(file)s", fileName);
      msg = msg.replace("%(line)d", java.lang.Integer.toString(sourceCoordinate.startLine));
      msg =
          msg.replace("%(column)d", java.lang.Integer.toString(sourceCoordinate.startColumn));
      msg = msg.replace("%(expected)s", expectedStr);
      msg = msg.replace("%(found)s", foundStr);
      return msg;
    }

    /**
     * Used by Language Server.
     */
    public SourceCoordinate getSourceCoordinate() {
      return sourceCoordinate;
    }
  }

  public static class ParseErrorWithSymbolList extends ParseError {
    private static final long  serialVersionUID = 561313162441723955L;
    private final List<Symbol> expectedSymbols;

    ParseErrorWithSymbolList(final String message, final List<Symbol> expected,
        final Parser parser) {
      super(message, null, parser);
      this.expectedSymbols = expected;
    }

    @Override
    protected String expectedSymbolAsString() {
      StringBuilder sb = new StringBuilder();
      String deliminator = "";

      for (Symbol s : expectedSymbols) {
        sb.append(deliminator);
        sb.append(s);
        deliminator = ", ";
      }
      return sb.toString();
    }
  }

  public Parser(final String content, final Source source,
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe,
      final Universe universe) {
    this.universe = universe;
    this.source = source;
    this.inlinableNodes = Primitives.inlinableNodes;
    this.structuralProbe = structuralProbe;

    sym = NONE;
    lexer = new Lexer(content);
    nextSym = NONE;
    getSymbolFromLexer();
  }

  protected SourceCoordinate getCoordinate() {
    SourceCoordinate coord = lexer.getStartCoordinate();
    // getSource(coord);
    return coord;
  }

  public void classdef(final ClassGenerationContext cgenc) throws ProgramDefinitionError {
    cgenc.setName(universe.symbolFor(text));
    SourceCoordinate coord = getCoordinate();
    expect(Identifier);
    expect(Equal);

    superclass(cgenc);

    expect(NewTerm);
    instanceFields(cgenc);

    while (isIdentifier(sym) || sym == Keyword || sym == OperatorSequence
        || symIn(binaryOpSyms)) {
      MethodGenerationContext mgenc = new MethodGenerationContext(cgenc, structuralProbe);

      ExpressionNode methodBody = method(mgenc);

      cgenc.addInstanceMethod(
          mgenc.assemble(methodBody, lastMethodsSourceSection, lastFullMethodsSourceSection));
    }

    if (accept(Separator)) {
      cgenc.setClassSide(true);
      classFields(cgenc);
      while (isIdentifier(sym) || sym == Keyword || sym == OperatorSequence
          || symIn(binaryOpSyms)) {
        MethodGenerationContext mgenc = new MethodGenerationContext(cgenc, structuralProbe);

        ExpressionNode methodBody = method(mgenc);

        cgenc.addClassMethod(mgenc.assemble(
            methodBody, lastMethodsSourceSection, lastFullMethodsSourceSection));
      }
    }
    expect(EndTerm);
    cgenc.setSourceSection(getSource(coord));
  }

  private void superclass(final ClassGenerationContext cgenc) throws ParseError {
    SSymbol superName;
    if (sym == Identifier) {
      superName = universe.symbolFor(text);
      accept(Identifier);
    } else {
      superName = universe.symbolFor("Object");
    }
    cgenc.setSuperName(superName);

    // Load the super class, if it is not nil (break the dependency cycle)
    if (!superName.getString().equals("nil")) {
      SClass superClass = universe.loadClass(superName);
      if (superClass == null) {
        throw new ParseError("Super class " + superName.getString() +
            " could not be loaded", NONE, this);
      }

      cgenc.setInstanceFieldsOfSuper(superClass.getInstanceFieldDefinitions());
      cgenc.setClassFieldsOfSuper(
          superClass.getSOMClass(universe).getInstanceFieldDefinitions());
    }
  }

  private boolean symIn(final List<Symbol> ss) {
    return ss.contains(sym);
  }

  private boolean accept(final Symbol s) {
    if (sym == s) {
      getSymbolFromLexer();
      return true;
    }
    return false;
  }

  private boolean acceptOneOf(final List<Symbol> ss) {
    if (symIn(ss)) {
      getSymbolFromLexer();
      return true;
    }
    return false;
  }

  private boolean expect(final Symbol s) throws ParseError {
    if (accept(s)) {
      return true;
    }

    throw new ParseError("Unexpected symbol. Expected %(expected)s, but found "
        + "%(found)s", s, this);
  }

  private boolean expectOneOf(final List<Symbol> ss) throws ParseError {
    if (acceptOneOf(ss)) {
      return true;
    }

    throw new ParseErrorWithSymbolList("Unexpected symbol. Expected one of " +
        "%(expected)s, but found %(found)s", ss, this);
  }

  private void instanceFields(final ClassGenerationContext cgenc)
      throws ProgramDefinitionError {
    if (accept(Or)) {
      while (isIdentifier(sym)) {
        SourceCoordinate coord = getCoordinate();
        String var = variable();
        cgenc.addInstanceField(universe.symbolFor(var), getSource(coord));
      }
      expect(Or);
    }
  }

  private void classFields(final ClassGenerationContext cgenc) throws ProgramDefinitionError {
    if (accept(Or)) {
      while (isIdentifier(sym)) {
        SourceCoordinate coord = getCoordinate();
        String var = variable();
        cgenc.addClassField(universe.symbolFor(var), getSource(coord));
      }
      expect(Or);
    }
  }

  private SourceSection getEmptySource() {
    SourceCoordinate coord = getCoordinate();
    return source.createSection(coord.charIndex, 0);
  }

  protected SourceSection getSource(final SourceCoordinate coord) {
    assert lexer.getNumberOfCharactersRead() - coord.charIndex >= 0;
    return source.createSection(coord.charIndex,
        Math.max(lexer.getNumberOfNonWhiteCharsRead() - coord.charIndex, 0));
  }

  private ExpressionNode method(final MethodGenerationContext mgenc)
      throws ProgramDefinitionError {
    lastCoordinate = getCoordinate();
    pattern(mgenc);
    expect(Equal);
    if (sym == Primitive) {
      mgenc.markAsPrimitive();
      primitiveBlock();
      return null;
    } else {
      return methodBlock(mgenc);
    }
  }

  private void primitiveBlock() throws ParseError {
    expect(Primitive);
    lastMethodsSourceSection = lastFullMethodsSourceSection = getSource(lastCoordinate);
  }

  private void pattern(final MethodGenerationContext mgenc) throws ProgramDefinitionError {
    // TODO: can we do that optionally?
    mgenc.addArgumentIfAbsent(universe.symSelf, getEmptySource());
    switch (sym) {
      case Identifier:
      case Primitive:
        unaryPattern(mgenc);
        break;
      case Keyword:
        keywordPattern(mgenc);
        break;
      default:
        binaryPattern(mgenc);
        break;
    }
  }

  protected void unaryPattern(final MethodGenerationContext mgenc) throws ParseError {
    mgenc.setSignature(unarySelector());
  }

  protected void binaryPattern(final MethodGenerationContext mgenc)
      throws ProgramDefinitionError {
    mgenc.setSignature(binarySelector());
    SourceCoordinate coord = getCoordinate();
    mgenc.addArgumentIfAbsent(universe.symbolFor(argument()), getSource(coord));
  }

  protected void keywordPattern(final MethodGenerationContext mgenc)
      throws ProgramDefinitionError {
    StringBuffer kw = new StringBuffer();
    do {
      kw.append(keyword());
      SourceCoordinate coord = getCoordinate();
      mgenc.addArgumentIfAbsent(universe.symbolFor(argument()), getSource(coord));
    } while (sym == Keyword);

    mgenc.setSignature(universe.symbolFor(kw.toString()));
  }

  private ExpressionNode methodBlock(final MethodGenerationContext mgenc)
      throws ProgramDefinitionError {
    expect(NewTerm);
    SourceCoordinate coord = getCoordinate();
    ExpressionNode methodBody = blockContents(mgenc);
    lastMethodsSourceSection = getSource(coord);
    lastFullMethodsSourceSection = getSource(lastCoordinate);
    expect(EndTerm);

    return methodBody;
  }

  protected SSymbol unarySelector() throws ParseError {
    return universe.symbolFor(identifier());
  }

  protected SSymbol binarySelector() throws ParseError {
    String s = new String(text);

    // Checkstyle: stop @formatter:off
    if (accept(Or)) {
    } else if (accept(Comma)) {
    } else if (accept(Minus)) {
    } else if (accept(Equal)) {
    } else if (acceptOneOf(singleOpSyms)) {
    } else if (accept(OperatorSequence)) {
    } else { expect(NONE); }
    // Checkstyle: resume @formatter:on

    return universe.symbolFor(s);
  }

  private String identifier() throws ParseError {
    String s = new String(text);
    boolean isPrimitive = accept(Primitive);
    if (!isPrimitive) {
      expect(Identifier);
    }
    return s;
  }

  protected String keyword() throws ParseError {
    String s = new String(text);
    expect(Keyword);

    return s;
  }

  private String argument() throws ProgramDefinitionError {
    return variable();
  }

  private ExpressionNode blockContents(final MethodGenerationContext mgenc)
      throws ProgramDefinitionError {
    if (accept(Or)) {
      locals(mgenc);
      expect(Or);
    }
    mgenc.setVarsOnMethodScope();
    return blockBody(mgenc);
  }

  private void locals(final MethodGenerationContext mgenc) throws ProgramDefinitionError {
    while (isIdentifier(sym)) {
      SourceCoordinate coord = getCoordinate();
      mgenc.addLocalIfAbsent(universe.symbolFor(variable()), getSource(coord));
    }
  }

  private ExpressionNode blockBody(final MethodGenerationContext mgenc)
      throws ProgramDefinitionError {
    SourceCoordinate coord = getCoordinate();
    List<ExpressionNode> expressions = new ArrayList<ExpressionNode>();

    boolean sawPeriod = true;

    while (true) {
      if (accept(Exit)) {
        if (!sawPeriod) {
          expect(Period);
        }
        expressions.add(result(mgenc));
        return createSequenceNode(coord, expressions);
      } else if (sym == EndBlock) {
        return createSequenceNode(coord, expressions);
      } else if (sym == EndTerm) {
        // the end of the method has been found (EndTerm) - make it implicitly return "self"
        ExpressionNode self =
            variableRead(mgenc, universe.symSelf, getSource(getCoordinate()));
        expressions.add(self);
        return createSequenceNode(coord, expressions);
      }

      if (!sawPeriod) {
        expect(Period);
      }

      expressions.add(expression(mgenc));
      sawPeriod = accept(Period);
    }
  }

  private ExpressionNode createSequenceNode(final SourceCoordinate coord,
      final List<ExpressionNode> expressions) {
    if (expressions.size() == 0) {
      return createGlobalRead("nil", universe, getSource(coord));
    } else if (expressions.size() == 1) {
      return expressions.get(0);
    }
    return createSequence(expressions, getSource(coord));
  }

  private ExpressionNode result(final MethodGenerationContext mgenc)
      throws ProgramDefinitionError {
    SourceCoordinate coord = getCoordinate();

    ExpressionNode exp = expression(mgenc);
    accept(Period);

    if (mgenc.isBlockMethod()) {
      return mgenc.getNonLocalReturn(exp, getSource(coord));
    } else {
      return exp;
    }
  }

  private ExpressionNode expression(final MethodGenerationContext mgenc)
      throws ProgramDefinitionError {
    peekForNextSymbolFromLexer();

    if (nextSym == Assign) {
      return assignation(mgenc);
    } else {
      return evaluation(mgenc);
    }
  }

  protected ExpressionNode assignation(final MethodGenerationContext mgenc)
      throws ProgramDefinitionError {
    return assignments(mgenc);
  }

  private ExpressionNode assignments(final MethodGenerationContext mgenc)
      throws ProgramDefinitionError {
    SourceCoordinate coord = getCoordinate();

    if (!isIdentifier(sym)) {
      throw new ParseError("Assignments should always target variables or" +
          " fields, but found instead a %(found)s",
          Symbol.Identifier, this);
    }
    SSymbol variable = universe.symbolFor(assignment());

    peekForNextSymbolFromLexer();

    ExpressionNode value;
    if (nextSym == Assign) {
      value = assignments(mgenc);
    } else {
      value = evaluation(mgenc);
    }

    return variableWrite(mgenc, variable, value, getSource(coord));
  }

  protected String assignment() throws ProgramDefinitionError {
    String v = variable();
    expect(Assign);
    return v;
  }

  private ExpressionNode evaluation(final MethodGenerationContext mgenc)
      throws ProgramDefinitionError {
    ExpressionNode exp = primary(mgenc);
    if (isIdentifier(sym) || sym == Keyword || sym == OperatorSequence
        || symIn(binaryOpSyms)) {
      exp = messages(mgenc, exp);
    }
    return exp;
  }

  private ExpressionNode primary(final MethodGenerationContext mgenc)
      throws ProgramDefinitionError {
    switch (sym) {
      case Identifier:
      case Primitive: {
        SourceCoordinate coord = getCoordinate();
        SSymbol v = universe.symbolFor(variable());
        return variableRead(mgenc, v, getSource(coord));
      }
      case NewTerm: {
        return nestedTerm(mgenc);
      }
      case NewBlock: {
        SourceCoordinate coord = getCoordinate();
        MethodGenerationContext bgenc = new MethodGenerationContext(mgenc.getHolder(), mgenc);

        ExpressionNode blockBody = nestedBlock(bgenc);

        SMethod blockMethod = (SMethod) bgenc.assemble(
            blockBody, lastMethodsSourceSection, lastMethodsSourceSection);
        mgenc.addEmbeddedBlockMethod(blockMethod);

        if (bgenc.requiresContext()) {
          return new BlockNodeWithContext(blockMethod, universe).initialize(getSource(coord));
        } else {
          return new BlockNode(blockMethod, universe).initialize(getSource(coord));
        }
      }
      default: {
        return literal();
      }
    }
  }

  private String variable() throws ProgramDefinitionError {
    return identifier();
  }

  private ExpressionNode messages(final MethodGenerationContext mgenc,
      final ExpressionNode receiver) throws ProgramDefinitionError {
    ExpressionNode msg;
    if (isIdentifier(sym)) {
      msg = unaryMessage(receiver);

      while (isIdentifier(sym)) {
        msg = unaryMessage(msg);
      }

      while (sym == OperatorSequence || symIn(binaryOpSyms)) {
        msg = binaryMessage(mgenc, msg);
      }

      if (sym == Keyword) {
        msg = keywordMessage(mgenc, msg);
      }
    } else if (sym == OperatorSequence || symIn(binaryOpSyms)) {
      msg = binaryMessage(mgenc, receiver);

      while (sym == OperatorSequence || symIn(binaryOpSyms)) {
        msg = binaryMessage(mgenc, msg);
      }

      if (sym == Keyword) {
        msg = keywordMessage(mgenc, msg);
      }
    } else {
      msg = keywordMessage(mgenc, receiver);
    }
    return msg;
  }

  protected ExpressionNode unaryMessage(final ExpressionNode receiver)
      throws ParseError {
    SourceCoordinate coord = getCoordinate();
    SSymbol selector = unarySelector();
    return createMessageSend(selector, new ExpressionNode[] {receiver},
        getSource(coord), universe);
  }

  protected ExpressionNode binaryMessage(final MethodGenerationContext mgenc,
      final ExpressionNode receiver) throws ProgramDefinitionError {
    SourceCoordinate coord = getCoordinate();
    SSymbol msg = binarySelector();
    ExpressionNode operand = binaryOperand(mgenc);

    return createMessageSend(msg, new ExpressionNode[] {receiver, operand},
        getSource(coord), universe);
  }

  protected ExpressionNode binaryOperand(final MethodGenerationContext mgenc)
      throws ProgramDefinitionError {
    ExpressionNode operand = primary(mgenc);

    // a binary operand can receive unaryMessages
    // Example: 2 * 3 asString
    // is evaluated as 2 * (3 asString)
    while (isIdentifier(sym)) {
      operand = unaryMessage(operand);
    }
    return operand;
  }

  protected ExpressionNode keywordMessage(final MethodGenerationContext mgenc,
      final ExpressionNode receiver) throws ProgramDefinitionError {
    SourceCoordinate coord = getCoordinate();
    List<ExpressionNode> arguments = new ArrayList<ExpressionNode>();
    StringBuffer kw = new StringBuffer();

    arguments.add(receiver);

    do {
      kw.append(keyword());
      arguments.add(formula(mgenc));
    } while (sym == Keyword);

    String msgStr = kw.toString();
    SSymbol msg = universe.symbolFor(msgStr);

    SourceSection source = getSource(coord);

    ExpressionNode inlined = inlinableNodes.inline(msg, arguments, mgenc, source);
    if (inlined != null) {
      return inlined;
    }

    return createMessageSend(msg, arguments.toArray(new ExpressionNode[0]),
        source, universe);
  }

  private ExpressionNode formula(final MethodGenerationContext mgenc)
      throws ProgramDefinitionError {
    ExpressionNode operand = binaryOperand(mgenc);

    while (sym == OperatorSequence || symIn(binaryOpSyms)) {
      operand = binaryMessage(mgenc, operand);
    }
    return operand;
  }

  private ExpressionNode nestedTerm(final MethodGenerationContext mgenc)
      throws ProgramDefinitionError {
    expect(NewTerm);
    ExpressionNode exp = expression(mgenc);
    expect(EndTerm);
    return exp;
  }

  private LiteralNode literal() throws ParseError {
    SourceCoordinate coord = getCoordinate();
    switch (sym) {
      case Pound: {
        peekForNextSymbolFromLexerIfNecessary();
        if (nextSym == NewTerm) {
          return new ArrayLiteralNode(literalArray()).initialize(getSource(coord));
        } else {
          return new SymbolLiteralNode(literalSymbol()).initialize(getSource(coord));
        }
      }
      case STString:
        return new StringLiteralNode(literalString()).initialize(getSource(coord));
      default:
        boolean isNegative = isNegativeNumber();
        if (sym == Integer) {
          Object value = literalInteger(isNegative);
          if (value instanceof BigInteger) {
            return new BigIntegerLiteralNode(
                (BigInteger) value).initialize(getSource(coord));
          } else {
            return new IntegerLiteralNode((Long) value).initialize(getSource(coord));
          }
        } else {
          if (sym != Double) {
            throw new ParseError("Unexpected symbol. Expected %(expected)s, but found "
                + "%(found)s", sym, this);
          }
          return new DoubleLiteralNode(literalDouble(isNegative)).initialize(getSource(coord));
        }
    }
  }

  private Object getObjectForCurrentLiteral() throws ParseError {
    switch (sym) {
      case Pound:
        peekForNextSymbolFromLexerIfNecessary();

        if (nextSym == NewTerm) {
          return literalArray();
        } else {
          return literalSymbol();
        }
      case STString:
        return literalString();
      case Minus:
        boolean isNegative = isNegativeNumber();
        if (sym == Integer) {
          return literalInteger(isNegative);
        } else if (sym == Double) {
          return literalDouble(isNegative);
        }
        throw new ParseError("Could not parse literal array value", sym, this);
      case Integer:
        return literalInteger(isNegativeNumber());
      case Double:
        return literalDouble(isNegativeNumber());
      case Identifier:
        expect(Identifier);
        return universe.getGlobal(universe.symbolFor(new String(text)));
      default:
        throw new ParseError("Could not parse literal array value", NONE, this);
    }
  }

  private boolean isNegativeNumber() throws ParseError {
    boolean isNegative = false;
    if (sym == Minus) {
      expect(Minus);
      isNegative = true;
    }
    return isNegative;
  }

  private Object literalInteger(final boolean isNegative) throws ParseError {
    try {
      long i = Long.parseLong(text);
      if (isNegative) {
        i = 0 - i;
      }
      expect(Integer);
      return i;
    } catch (NumberFormatException e) {
      try {
        BigInteger big = new BigInteger(text);
        if (isNegative) {
          big = big.negate();
        }
        expect(Integer);
        return big;
      } catch (NumberFormatException e2) {
        throw new ParseError("Could not parse integer. Expected a number but " +
            "got '" + text + "'", NONE, this);
      }
    }
  }

  private double literalDouble(final boolean isNegative) throws ParseError {
    try {
      double d = java.lang.Double.parseDouble(text);
      if (isNegative) {
        d = 0.0 - d;
      }
      expect(Double);
      return d;
    } catch (NumberFormatException e) {
      throw new ParseError("Could not parse double. Expected a number but " +
          "got '" + text + "'", NONE, this);
    }
  }

  private SSymbol literalSymbol() throws ParseError {
    SSymbol symb;
    expect(Pound);
    if (sym == STString) {
      String s = string();
      symb = universe.symbolFor(s);
    } else {
      symb = selector();
    }
    return symb;
  }

  private SArray literalArray() throws ParseError {
    List<Object> literals = new ArrayList<Object>();
    expect(Pound);
    expect(NewTerm);
    while (sym != EndTerm) {
      literals.add(getObjectForCurrentLiteral());
    }
    expect(EndTerm);
    return SArray.create(literals.toArray());
  }

  private String literalString() throws ParseError {
    return string();
  }

  protected SSymbol selector() throws ParseError {
    if (sym == OperatorSequence || symIn(singleOpSyms)) {
      return binarySelector();
    } else if (sym == Keyword || sym == KeywordSequence) {
      return keywordSelector();
    } else {
      return unarySelector();
    }
  }

  private SSymbol keywordSelector() throws ParseError {
    String s = new String(text);
    expectOneOf(keywordSelectorSyms);
    SSymbol symb = universe.symbolFor(s);
    return symb;
  }

  private String string() throws ParseError {
    String s = new String(text);
    expect(STString);
    return s;
  }

  private ExpressionNode nestedBlock(final MethodGenerationContext mgenc)
      throws ProgramDefinitionError {
    expect(NewBlock);
    SourceCoordinate coord = getCoordinate();

    mgenc.addArgumentIfAbsent(universe.symBlockSelf, getEmptySource());

    if (sym == Colon) {
      blockPattern(mgenc);
    }

    // generate Block signature
    String blockSig =
        "$blockMethod@" + lexer.getCurrentLineNumber() + "@" + lexer.getCurrentColumn();
    int argSize = mgenc.getNumberOfArguments();
    for (int i = 1; i < argSize; i++) {
      blockSig += ":";
    }

    mgenc.setSignature(universe.symbolFor(blockSig));

    ExpressionNode expressions = blockContents(mgenc);

    lastMethodsSourceSection = getSource(coord);

    expect(EndBlock);

    return expressions;
  }

  private void blockPattern(final MethodGenerationContext mgenc)
      throws ProgramDefinitionError {
    blockArguments(mgenc);
    expect(Or);
  }

  private void blockArguments(final MethodGenerationContext mgenc)
      throws ProgramDefinitionError {
    do {
      expect(Colon);
      SourceCoordinate coord = getCoordinate();
      mgenc.addArgumentIfAbsent(universe.symbolFor(argument()), getSource(coord));
    } while (sym == Colon);
  }

  protected ExpressionNode variableRead(final MethodGenerationContext mgenc,
      final SSymbol variableName, final SourceSection source) {
    // we need to handle super special here
    if (universe.symSuper == variableName) {
      return mgenc.getSuperReadNode(source);
    }

    // now look up first local variables, or method arguments
    Variable variable = mgenc.getVariable(variableName);
    if (variable != null) {
      return mgenc.getLocalReadNode(variableName, source);
    }

    // then object fields
    FieldReadNode fieldRead = mgenc.getObjectFieldRead(variableName, source);

    if (fieldRead != null) {
      return fieldRead;
    }

    // and finally assume it is a global
    return mgenc.getGlobalRead(variableName, universe, source);
  }

  private ExpressionNode variableWrite(final MethodGenerationContext mgenc,
      final SSymbol variableName, final ExpressionNode exp, final SourceSection source) {
    Variable variable = mgenc.getVariable(variableName);
    if (variable != null) {
      return mgenc.getLocalWriteNode(variableName, exp, source);
    }

    FieldWriteNode fieldWrite = mgenc.getObjectFieldWrite(variableName, exp, universe, source);

    if (fieldWrite != null) {
      return fieldWrite;
    } else {
      throw new RuntimeException("Neither a variable nor a field found "
          + "in current scope that is named " + variableName + ". Arguments are read-only.");
    }
  }

  private void getSymbolFromLexer() {
    sym = lexer.getSym();
    text = lexer.getText();
  }

  private void peekForNextSymbolFromLexerIfNecessary() {
    if (!lexer.getPeekDone()) {
      peekForNextSymbolFromLexer();
    }
  }

  private void peekForNextSymbolFromLexer() {
    Peek peek = lexer.peek();
    nextSym = peek.nextSym;
  }

  private static boolean isIdentifier(final Symbol sym) {
    return sym == Identifier || sym == Primitive;
  }

  private static boolean printableSymbol(final Symbol sym) {
    return sym == Integer || sym == Double || sym.compareTo(STString) >= 0;
  }
}
