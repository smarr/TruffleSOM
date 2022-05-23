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
import static trufflesom.vm.SymbolTable.symBlockSelf;
import static trufflesom.vm.SymbolTable.symNil;
import static trufflesom.vm.SymbolTable.symObject;
import static trufflesom.vm.SymbolTable.symSelf;
import static trufflesom.vm.SymbolTable.symbolFor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.source.Source;

import bdt.basic.ProgramDefinitionError;
import bdt.source.SourceCoordinate;
import bdt.tools.structure.StructuralProbe;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.FieldNode.FieldReadNode;
import trufflesom.interpreter.nodes.GlobalNode;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SSymbol;


public abstract class Parser<MGenC extends MethodGenerationContext> {

  protected final Lexer  lexer;
  protected final Source source;

  protected final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe;

  protected Symbol sym;
  protected String text;
  protected Symbol nextSym;

  protected int lastStartIndex;

  protected long lastMethodsCoord;

  private static final List<Symbol>   singleOpSyms        = new ArrayList<Symbol>();
  protected static final List<Symbol> binaryOpSyms        = new ArrayList<Symbol>();
  private static final List<Symbol>   keywordSelectorSyms = new ArrayList<Symbol>();

  protected boolean superSend;

  static {
    for (Symbol s : new Symbol[] {Not, And, Or, Star, Div, Mod, Plus, Equal,
        More, Less, Comma, At, Per}) {
      singleOpSyms.add(s);
    }
    for (Symbol s : new Symbol[] {Or, Comma, Minus, Equal, Not, And, Or, Star,
        Div, Mod, Plus, Equal, More, Less, Comma, At, Per}) {
      binaryOpSyms.add(s);
    }
    for (Symbol s : new Symbol[] {Keyword, KeywordSequence}) {
      keywordSelectorSyms.add(s);
    }
  }

  @Override
  public String toString() {
    String name = source.getName();
    String loc = SourceCoordinate.getLocationQualifier(getStartIndex(), source);
    return "Parser(" + name + loc + ")";
  }

  public static class ParseError extends ProgramDefinitionError {
    private static final long serialVersionUID = 425390202979033628L;

    private final int startIndex;

    private final Source source;
    private final String text;
    private final String rawBuffer;
    private final String fileName;
    private final Symbol expected;
    private final Symbol found;

    public ParseError(final String message, final Symbol expected, final Parser<?> parser) {
      super(message);
      this.source = parser.source;
      this.startIndex = parser.getStartIndex();
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
      msg = msg.replace("%(line)d", "" + getLine());
      msg =
          msg.replace("%(column)d", "" + getColumn());
      if (expectedStr != null) {
        msg = msg.replace("%(expected)s", expectedStr);
      }
      msg = msg.replace("%(found)s", foundStr);
      return msg;
    }

    /**
     * Used by Language Server.
     */
    public int getLine() {
      return source.getLineNumber(startIndex);
    }

    /**
     * Used by Language Server.
     */
    public int getColumn() {
      return source.getColumnNumber(startIndex);
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
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe) {
    this.source = source;
    this.structuralProbe = structuralProbe;

    sym = NONE;
    lexer = createLexer(content);
    nextSym = NONE;
    getSymbolFromLexer();
  }

  protected Lexer createLexer(final String content) {
    return new Lexer(content);
  }

  public Source getSource() {
    return source;
  }

  protected int getStartIndex() {
    return lexer.getNumberOfCharactersRead();
  }

  protected abstract MGenC createMGenC(ClassGenerationContext cgenc,
      StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe);

  public void classdef(final ClassGenerationContext cgenc) throws ProgramDefinitionError {
    int coord = getStartIndex();

    className(cgenc, coord);
    expect(Equal);

    superclass(cgenc);

    expect(NewTerm);
    instanceFields(cgenc);

    while (isIdentifier(sym) || sym == Keyword || sym == OperatorSequence
        || symIn(binaryOpSyms)) {
      MGenC mgenc = createMGenC(cgenc, structuralProbe);

      ExpressionNode methodBody = method(mgenc);

      cgenc.addInstanceMethod(mgenc.assemble(methodBody, lastMethodsCoord), this);
    }

    classSide(cgenc);
    expect(EndTerm);
    cgenc.setSourceCoord(getCoordWithLength(coord));
  }

  protected void classSide(final ClassGenerationContext cgenc)
      throws ProgramDefinitionError, ParseError {
    if (accept(Separator)) {
      cgenc.switchToClassSide();
      classFields(cgenc);
      while (isIdentifier(sym) || sym == Keyword || sym == OperatorSequence
          || symIn(binaryOpSyms)) {
        MGenC mgenc = createMGenC(cgenc, structuralProbe);

        ExpressionNode methodBody = method(mgenc);

        cgenc.addClassMethod(mgenc.assemble(methodBody, lastMethodsCoord), this);
      }
    }
  }

  protected void className(final ClassGenerationContext cgenc, final int coord)
      throws ParseError {
    cgenc.setName(symbolFor(text));

    if ("Object".equals(text)) {
      Universe.selfCoord = getCoordWithLength(coord);
      Universe.selfSource = source;
    }

    expect(Identifier);
  }

  private void superclass(final ClassGenerationContext cgenc) throws ParseError {
    SSymbol superName;
    if (sym == Identifier) {
      superName = symbolFor(text);
      accept(Identifier);
    } else {
      superName = symObject;
    }

    // Load the super class, if it is not nil (break the dependency cycle)
    if (superName != symNil) {
      SClass superClass = Universe.loadClass(superName);
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

  protected SSymbol field() throws ParseError {
    return identifier();
  }

  private void instanceFields(final ClassGenerationContext cgenc)
      throws ProgramDefinitionError {
    if (accept(Or)) {
      while (isIdentifier(sym)) {
        int coord = getStartIndex();
        SSymbol var = field();
        cgenc.addInstanceField(var, getCoordWithLength(coord));
      }
      expect(Or);
    }
  }

  private void classFields(final ClassGenerationContext cgenc) throws ProgramDefinitionError {
    if (accept(Or)) {
      while (isIdentifier(sym)) {
        int coord = getStartIndex();
        SSymbol var = field();
        cgenc.addClassField(var, getCoordWithLength(coord));
      }
      expect(Or);
    }
  }

  protected long getEmptyCoord() {
    int coord = getStartIndex();
    return SourceCoordinate.withZeroLength(coord);
  }

  protected long getCoordWithLength(final int startIndex) {
    int length = Math.max(lexer.getNumberOfNonWhiteCharsRead() - startIndex, 0);
    return SourceCoordinate.create(startIndex, length);
  }

  public ExpressionNode method(final MGenC mgenc)
      throws ProgramDefinitionError {
    lastStartIndex = getStartIndex();
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
    int coord = getStartIndex();
    ExpressionNode methodBody = blockContents(mgenc);
    lastMethodsCoord = getCoordWithLength(coord);
    expect(EndTerm);

    return methodBody;
  }

  protected void primitiveBlock() throws ParseError {
    expect(Primitive);
    lastMethodsCoord = getCoordWithLength(lastStartIndex);
  }

  public ExpressionNode nestedBlock(final MGenC mgenc) throws ProgramDefinitionError {
    expect(NewBlock);
    int coord = getStartIndex();

    mgenc.addArgumentIfAbsent(symBlockSelf, getEmptyCoord());

    if (sym == Colon) {
      blockPattern(mgenc);
    }

    mgenc.setBlockSignature(source, coord);

    ExpressionNode expressions = blockContents(mgenc);

    lastMethodsCoord = getCoordWithLength(coord);

    expect(EndBlock);

    return expressions;
  }

  private void pattern(final MGenC mgenc) throws ProgramDefinitionError {
    assert Universe.selfSource != null;
    mgenc.addArgumentIfAbsent(symSelf, Universe.selfCoord);
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
    int coord = getStartIndex();
    mgenc.addArgumentIfAbsent(argument(), getCoordWithLength(coord));
  }

  protected void keywordPattern(final MGenC mgenc) throws ProgramDefinitionError {
    StringBuilder kw = new StringBuilder();
    do {
      kw.append(keyword());
      int coord = getStartIndex();
      mgenc.addArgumentIfAbsent(argument(), getCoordWithLength(coord));
    } while (sym == Keyword);

    mgenc.setSignature(symbolFor(kw.toString()));
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

    return symbolFor(s);
  }

  private SSymbol identifier() throws ParseError {
    String s = new String(text);
    boolean isPrimitive = accept(Primitive);
    if (!isPrimitive) {
      expect(Identifier);
    }
    return symbolFor(s);
  }

  protected String keyword() throws ParseError {
    String s = new String(text);
    expect(Keyword);

    return s;
  }

  protected SSymbol argument() throws ProgramDefinitionError {
    return variable();
  }

  protected SSymbol local() throws ProgramDefinitionError {
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
      int coord = getStartIndex();
      SSymbol var = variable();
      if (mgenc.hasLocal(var)) {
        throw new ParseError("Declared the variable " + var.getString() + " multiple times.",
            null, this);
      }
      mgenc.addLocal(var, getCoordWithLength(coord));
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
      symb = symbolFor(s);
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
    SSymbol symb = symbolFor(s);
    return symb;
  }

  protected String string() throws ParseError {
    String s = new String(text);
    expect(STString);
    return s;
  }

  protected void blockPattern(final MGenC mgenc) throws ProgramDefinitionError {
    blockArguments(mgenc);
    expect(Or);
  }

  private void blockArguments(final MGenC mgenc) throws ProgramDefinitionError {
    do {
      expect(Colon);
      int coord = getStartIndex();
      mgenc.addArgumentIfAbsent(argument(), getCoordWithLength(coord));
    } while (sym == Colon);
  }

  protected ExpressionNode variableRead(final MGenC mgenc, final SSymbol variableName,
      final long coord) {
    // now look up first local variables, or method arguments
    Variable variable = mgenc.getVariable(variableName);
    if (variable != null) {
      return mgenc.getLocalReadNode(variable, coord);
    }

    // then object fields
    FieldReadNode fieldRead = mgenc.getObjectFieldRead(variableName, coord);

    if (fieldRead != null) {
      return fieldRead;
    }

    // and finally assume it is a global
    return GlobalNode.create(variableName, mgenc).initialize(coord);
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
    nextSym = lexer.peek();
  }

  protected static boolean isIdentifier(final Symbol sym) {
    return sym == Identifier || sym == Primitive;
  }

  private static boolean printableSymbol(final Symbol sym) {
    return sym == Integer || sym == Double || sym.compareTo(STString) >= 0;
  }
}
