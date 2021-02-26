package trufflesom.compiler;

import static trufflesom.compiler.Symbol.Assign;
import static trufflesom.compiler.Symbol.Double;
import static trufflesom.compiler.Symbol.EndBlock;
import static trufflesom.compiler.Symbol.EndTerm;
import static trufflesom.compiler.Symbol.Exit;
import static trufflesom.compiler.Symbol.Identifier;
import static trufflesom.compiler.Symbol.Integer;
import static trufflesom.compiler.Symbol.Keyword;
import static trufflesom.compiler.Symbol.NONE;
import static trufflesom.compiler.Symbol.NewTerm;
import static trufflesom.compiler.Symbol.OperatorSequence;
import static trufflesom.compiler.Symbol.Period;
import static trufflesom.compiler.Symbol.Pound;
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
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.FieldNode.FieldWriteNode;
import trufflesom.interpreter.nodes.GlobalNode;
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


public class ParserAst extends Parser<MethodGenerationContext> {

  private final InlinableNodes<SSymbol> inlinableNodes;

  public ParserAst(final String content, final Source source,
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe,
      final Universe universe) {
    super(content, source, structuralProbe, universe);
    this.inlinableNodes = Primitives.inlinableNodes;
  }

  @Override
  protected MethodGenerationContext createMGenC(final ClassGenerationContext cgenc,
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe) {
    return new MethodGenerationContext(cgenc, structuralProbe);
  }

  @Override
  protected ExpressionNode blockBody(final MethodGenerationContext mgenc,
      final boolean seenPeriod) throws ProgramDefinitionError {
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
      return GlobalNode.create(universe.symNil, universe, getSource(coord));
    } else if (expressions.size() == 1) {
      return expressions.get(0);
    }
    return createSequence(expressions, getSource(coord));
  }

  @Override
  protected ExpressionNode result(final MethodGenerationContext mgenc)
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

  @Override
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
    SSymbol variable = assignment();

    peekForNextSymbolFromLexer();

    ExpressionNode value;
    if (nextSym == Assign) {
      value = assignments(mgenc);
    } else {
      value = evaluation(mgenc);
    }

    return variableWrite(mgenc, variable, value, getSource(coord));
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

  @Override
  protected ExpressionNode evaluation(final MethodGenerationContext mgenc)
      throws ProgramDefinitionError {
    ExpressionNode exp = primary(mgenc);
    if (isIdentifier(sym) || sym == Keyword || sym == OperatorSequence
        || symIn(binaryOpSyms)) {
      exp = messages(mgenc, exp);
    }
    return exp;
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

  protected ExpressionNode keywordMessage(final MethodGenerationContext mgenc,
      final ExpressionNode receiver) throws ProgramDefinitionError {
    SourceCoordinate coord = getCoordinate();
    List<ExpressionNode> arguments = new ArrayList<ExpressionNode>();
    StringBuilder kw = new StringBuilder();

    arguments.add(receiver);

    do {
      kw.append(keyword());
      arguments.add(formula(mgenc));
    } while (sym == Keyword);

    SSymbol msg = universe.symbolFor(kw.toString());

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
        return universe.getGlobal(universe.symbolFor(text));
      default:
        throw new ParseError("Could not parse literal array value", NONE, this);
    }
  }

  private ExpressionNode primary(final MethodGenerationContext mgenc)
      throws ProgramDefinitionError {
    switch (sym) {
      case Identifier:
      case Primitive: {
        SourceCoordinate coord = getCoordinate();
        SSymbol v = variable();
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
}
