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
import static trufflesom.compiler.Symbol.Plus;
import static trufflesom.compiler.Symbol.Pound;
import static trufflesom.compiler.Symbol.Primitive;
import static trufflesom.compiler.Symbol.STString;
import static trufflesom.compiler.Symbol.Separator;
import static trufflesom.compiler.Symbol.Star;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import bd.basic.ProgramDefinitionError;
import bd.source.SourceCoordinate;
import bd.tools.structure.StructuralProbe;
import trufflesom.compiler.Lexer.Peek;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.FieldNode.FieldReadNode;
import trufflesom.interpreter.nodes.GlobalNode;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SSymbol;


public abstract class Parser<MGenC extends MethodGenerationContext> {

  protected final Universe universe;
  protected final Lexer    lexer;
  private final Source     source;

  protected final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe;

  protected Symbol sym;
  protected String text;
  protected Symbol nextSym;

  protected SourceCoordinate lastCoordinate;
  protected SourceSection    lastMethodsSourceSection;
  protected SourceSection    lastFullMethodsSourceSection;

  private static final List<Symbol>   singleOpSyms        = new ArrayList<Symbol>();
  protected static final List<Symbol> binaryOpSyms        = new ArrayList<Symbol>();
  private static final List<Symbol>   keywordSelectorSyms = new ArrayList<Symbol>();

  protected boolean superSend;

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

    public ParseError(final String message, final Symbol expected, final Parser<?> parser) {
      super(message);
      this.sourceCoordinate = parser.getCoordinate();
      this.text = parser.text;
      this.rawBuffer = parser.lexer.getCurrentLine();
      this.fileName = parser.source.getName();
      this.expected = expected;
      this.found = parser.sym;
    }

    protected String expectedSymbolAsString() {
      if (expected == null) {
        return null;
      }
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
      if (expectedStr != null) {
        msg = msg.replace("%(expected)s", expectedStr);
      }
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
        final Parser<?> parser) {
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

  protected Parser(final String content, final Source source,
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe,
      final Universe universe) {
    this.universe = universe;
    this.source = source;
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

  protected abstract MGenC createMGenC(ClassGenerationContext cgenc,
      StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe);

  public void classdef(final ClassGenerationContext cgenc) throws ProgramDefinitionError {
    cgenc.setName(universe.symbolFor(text));
    SourceCoordinate coord = getCoordinate();
    if ("Object".equals(text)) {
      universe.selfSource = getSource(coord);
    }

    expect(Identifier);
    expect(Equal);

    superclass(cgenc);

    expect(NewTerm);
    instanceFields(cgenc);

    while (isIdentifier(sym) || sym == Keyword || sym == OperatorSequence
        || symIn(binaryOpSyms)) {
      MGenC mgenc = createMGenC(cgenc, structuralProbe);

      ExpressionNode methodBody = method(mgenc);

      cgenc.addInstanceMethod(
          mgenc.assemble(methodBody, lastMethodsSourceSection, lastFullMethodsSourceSection));
    }

    if (accept(Separator)) {
      cgenc.switchToClassSide();
      classFields(cgenc);
      while (isIdentifier(sym) || sym == Keyword || sym == OperatorSequence
          || symIn(binaryOpSyms)) {
        MGenC mgenc = createMGenC(cgenc, structuralProbe);

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

    // Load the super class, if it is not nil (break the dependency cycle)
    if (!superName.getString().equals("nil")) {
      SClass superClass = universe.loadClass(superName);
      if (superClass == null) {
        throw new ParseError("Super class " + superName.getString() +
            " could not be loaded", NONE, this);
      }

      cgenc.setSuperClass(superClass);
    }
  }

  protected boolean symIn(final List<Symbol> ss) {
    return ss.contains(sym);
  }

  protected boolean accept(final Symbol s) {
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

  protected boolean expect(final Symbol s) throws ParseError {
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
        SSymbol var = variable();
        cgenc.addInstanceField(var, getSource(coord));
      }
      expect(Or);
    }
  }

  private void classFields(final ClassGenerationContext cgenc) throws ProgramDefinitionError {
    if (accept(Or)) {
      while (isIdentifier(sym)) {
        SourceCoordinate coord = getCoordinate();
        SSymbol var = variable();
        cgenc.addClassField(var, getSource(coord));
      }
      expect(Or);
    }
  }

  protected SourceSection getEmptySource() {
    SourceCoordinate coord = getCoordinate();
    return source.createSection(coord.charIndex, 0);
  }

  protected SourceSection getSource(final SourceCoordinate coord) {
    assert lexer.getNumberOfCharactersRead() - coord.charIndex >= 0;
    return source.createSection(coord.charIndex,
        Math.max(lexer.getNumberOfNonWhiteCharsRead() - coord.charIndex, 0));
  }

  private ExpressionNode method(final MGenC mgenc)
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

  protected ExpressionNode methodBlock(final MGenC mgenc) throws ProgramDefinitionError {
    expect(NewTerm);
    SourceCoordinate coord = getCoordinate();
    ExpressionNode methodBody = blockContents(mgenc);
    lastMethodsSourceSection = getSource(coord);
    lastFullMethodsSourceSection = getSource(lastCoordinate);
    expect(EndTerm);

    return methodBody;
  }

  private void primitiveBlock() throws ParseError {
    expect(Primitive);
    lastMethodsSourceSection = lastFullMethodsSourceSection = getSource(lastCoordinate);
  }

  protected ExpressionNode nestedBlock(final MGenC mgenc) throws ProgramDefinitionError {
    expect(NewBlock);
    SourceCoordinate coord = getCoordinate();

    mgenc.addArgumentIfAbsent(universe.symBlockSelf, getEmptySource());

    if (sym == Colon) {
      blockPattern(mgenc);
    }

    mgenc.setSignature(createBlockSignature(mgenc));

    ExpressionNode expressions = blockContents(mgenc);

    lastMethodsSourceSection = getSource(coord);

    expect(EndBlock);

    return expressions;
  }

  private void pattern(final MGenC mgenc) throws ProgramDefinitionError {
    assert universe.selfSource != null;
    mgenc.addArgumentIfAbsent(universe.symSelf, universe.selfSource);
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

  protected void unaryPattern(final MGenC mgenc) throws ParseError {
    mgenc.setSignature(unarySelector());
  }

  protected void binaryPattern(final MGenC mgenc) throws ProgramDefinitionError {
    mgenc.setSignature(binarySelector());
    SourceCoordinate coord = getCoordinate();
    mgenc.addArgumentIfAbsent(argument(), getSource(coord));
  }

  protected void keywordPattern(final MGenC mgenc) throws ProgramDefinitionError {
    StringBuilder kw = new StringBuilder();
    do {
      kw.append(keyword());
      SourceCoordinate coord = getCoordinate();
      mgenc.addArgumentIfAbsent(argument(), getSource(coord));
    } while (sym == Keyword);

    mgenc.setSignature(universe.symbolFor(kw.toString()));
  }

  protected SSymbol unarySelector() throws ParseError {
    return identifier();
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

  private SSymbol identifier() throws ParseError {
    String s = new String(text);
    boolean isPrimitive = accept(Primitive);
    if (!isPrimitive) {
      expect(Identifier);
    }
    return universe.symbolFor(s);
  }

  protected String keyword() throws ParseError {
    String s = new String(text);
    expect(Keyword);

    return s;
  }

  private SSymbol argument() throws ProgramDefinitionError {
    return variable();
  }

  protected ExpressionNode blockContents(final MGenC mgenc)
      throws ProgramDefinitionError {
    if (accept(Or)) {
      locals(mgenc);
      expect(Or);
    }
    mgenc.setVarsOnMethodScope();
    return blockBody(mgenc, false);
  }

  private void locals(final MGenC mgenc) throws ProgramDefinitionError {
    while (isIdentifier(sym)) {
      SourceCoordinate coord = getCoordinate();
      SSymbol var = variable();
      if (mgenc.hasLocal(var)) {
        throw new ParseError("Declared the variable " + var.getString() + " multiple times.",
            null, this);
      }
      mgenc.addLocal(var, getSource(coord));
    }
  }

  protected abstract ExpressionNode blockBody(MGenC mgenc, boolean seenPeriod)
      throws ProgramDefinitionError;

  protected abstract ExpressionNode result(MGenC mgenc) throws ProgramDefinitionError;

  protected ExpressionNode expression(final MGenC mgenc)
      throws ProgramDefinitionError {
    peekForNextSymbolFromLexerIfNecessary();

    if (nextSym == Assign) {
      return assignation(mgenc);
    } else {
      return evaluation(mgenc);
    }
  }

  protected abstract ExpressionNode assignation(MGenC mgenc) throws ProgramDefinitionError;

  protected abstract ExpressionNode evaluation(MGenC mgenc) throws ProgramDefinitionError;

  protected SSymbol assignment() throws ParseError {
    SSymbol v = variable();
    expect(Assign);
    return v;
  }

  protected SSymbol variable() throws ParseError {
    return identifier();
  }

  protected ExpressionNode nestedTerm(final MGenC mgenc) throws ProgramDefinitionError {
    expect(NewTerm);
    ExpressionNode exp = expression(mgenc);
    expect(EndTerm);
    return exp;
  }

  protected boolean isNegativeNumber() throws ParseError {
    boolean isNegative = false;
    if (sym == Minus) {
      expect(Minus);
      isNegative = true;
    }
    return isNegative;
  }

  protected Object literalInteger(final boolean isNegative) throws ParseError {
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

  protected double literalDouble(final boolean isNegative) throws ParseError {
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

  protected SSymbol literalSymbol() throws ParseError {
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

  protected String literalString() throws ParseError {
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

  protected SSymbol createBlockSignature(final MGenC mgenc) {
    String blockSig =
        "$blockMethod@" + lexer.getCurrentLineNumber() + "@" + lexer.getCurrentColumn();
    int argSize = mgenc.getNumberOfArguments();
    for (int i = 1; i < argSize; i++) {
      blockSig += ":";
    }

    return universe.symbolFor(blockSig);
  }

  protected void blockPattern(final MGenC mgenc) throws ProgramDefinitionError {
    blockArguments(mgenc);
    expect(Or);
  }

  private void blockArguments(final MGenC mgenc) throws ProgramDefinitionError {
    do {
      expect(Colon);
      SourceCoordinate coord = getCoordinate();
      mgenc.addArgumentIfAbsent(argument(), getSource(coord));
    } while (sym == Colon);
  }

  protected ExpressionNode variableRead(final MGenC mgenc, final SSymbol variableName,
      final SourceSection source) {
    // now look up first local variables, or method arguments
    Variable variable = mgenc.getVariable(variableName);
    if (variable != null) {
      return mgenc.getLocalReadNode(variable, source);
    }

    // then object fields
    FieldReadNode fieldRead = mgenc.getObjectFieldRead(variableName, source);

    if (fieldRead != null) {
      return fieldRead;
    }

    // and finally assume it is a global
    return GlobalNode.create(variableName, universe).initialize(source);
  }

  private void getSymbolFromLexer() {
    sym = lexer.getSym();
    text = lexer.getText();
  }

  protected void peekForNextSymbolFromLexerIfNecessary() {
    if (!lexer.getPeekDone()) {
      peekForNextSymbolFromLexer();
    }
  }

  protected void peekForNextSymbolFromLexer() {
    Peek peek = lexer.peek();
    nextSym = peek.nextSym;
  }

  protected static boolean isIdentifier(final Symbol sym) {
    return sym == Identifier || sym == Primitive;
  }

  private static boolean printableSymbol(final Symbol sym) {
    return sym == Integer || sym == Double || sym.compareTo(STString) >= 0;
  }
}
