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
import static trufflesom.interpreter.SNodeFactory.createSequence;
import static trufflesom.vm.SymbolTable.symNil;
import static trufflesom.vm.SymbolTable.symSelf;
import static trufflesom.vm.SymbolTable.symSuper;
import static trufflesom.vm.SymbolTable.symbolFor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.source.Source;

import bd.basic.ProgramDefinitionError;
import bd.inlining.InlinableNodes;
import bd.tools.structure.StructuralProbe;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.FieldNode;
import trufflesom.interpreter.nodes.GlobalNode;
import trufflesom.interpreter.nodes.MessageSendNode;
import trufflesom.interpreter.nodes.literals.BlockNode;
import trufflesom.interpreter.nodes.literals.BlockNode.BlockNodeWithContext;
import trufflesom.interpreter.nodes.literals.DoubleLiteralNode;
import trufflesom.interpreter.nodes.literals.GenericLiteralNode;
import trufflesom.interpreter.nodes.literals.IntegerLiteralNode;
import trufflesom.interpreter.nodes.literals.LiteralNode;
import trufflesom.interpreter.nodes.specialized.IntIncrementNodeGen;
import trufflesom.primitives.Primitives;
import trufflesom.vm.Globals;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SSymbol;


public class ParserAst extends Parser<MethodGenerationContext> {

  private final InlinableNodes<SSymbol> inlinableNodes;

  public ParserAst(final String content, final Source source,
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe) {
    super(content, source, structuralProbe);
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
    int coord = getStartIndex();
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
            variableRead(mgenc, symSelf, getCoordWithLength(getStartIndex()));
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

  private ExpressionNode createSequenceNode(final int coord,
      final List<ExpressionNode> expressions) {
    if (expressions.size() == 0) {
      return GlobalNode.create(symNil, null).initialize(getCoordWithLength(coord));
    } else if (expressions.size() == 1) {
      return expressions.get(0);
    }
    return createSequence(expressions, getCoordWithLength(coord));
  }

  @Override
  protected ExpressionNode result(final MethodGenerationContext mgenc)
      throws ProgramDefinitionError {
    int coord = getStartIndex();

    ExpressionNode exp = expression(mgenc);
    accept(Period);

    if (mgenc.isBlockMethod()) {
      return mgenc.getNonLocalReturn(exp, getCoordWithLength(coord));
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
    int coord = getStartIndex();

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

    return variableWrite(mgenc, variable, value, getCoordWithLength(coord));
  }

  private ExpressionNode variableWrite(final MethodGenerationContext mgenc,
      final SSymbol variableName, final ExpressionNode exp, final long coord) {
    Variable variable = mgenc.getVariable(variableName);
    if (variable != null) {
      return mgenc.getLocalWriteNode(variable, exp, coord);
    }

    FieldNode fieldWrite = mgenc.getObjectFieldWrite(variableName, exp, coord);

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

    superSend = false;
    return exp;
  }

  private ExpressionNode messages(final MethodGenerationContext mgenc,
      final ExpressionNode receiver) throws ProgramDefinitionError {
    ExpressionNode msg;
    if (isIdentifier(sym)) {
      msg = unaryMessage(mgenc, receiver);

      while (isIdentifier(sym)) {
        msg = unaryMessage(mgenc, msg);
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

  protected ExpressionNode unaryMessage(final MethodGenerationContext mgenc,
      final ExpressionNode receiver)
      throws ParseError {
    boolean isSuperSend = superSend;
    superSend = false;

    int coord = getStartIndex();
    SSymbol selector = unarySelector();

    ExpressionNode[] args = new ExpressionNode[] {receiver};
    long coordWithL = getCoordWithLength(coord);

    if (isSuperSend) {
      return MessageSendNode.createSuperSend(
          mgenc.getHolder().getSuperClass(), selector, args, coordWithL);
    }
    return MessageSendNode.create(selector, args, coordWithL);
  }

  protected ExpressionNode binaryMessage(final MethodGenerationContext mgenc,
      final ExpressionNode receiver) throws ProgramDefinitionError {
    boolean isSuperSend = superSend;
    superSend = false;
    int coord = getStartIndex();
    SSymbol msg = binarySelector();
    ExpressionNode operand = binaryOperand(mgenc);

    long coordWithL = getCoordWithLength(coord);

    ExpressionNode inlined =
        inlinableNodes.inline(msg, Arrays.asList(receiver, operand), mgenc, coordWithL);
    if (inlined != null) {
      assert !isSuperSend;
      return inlined;
    }

    ExpressionNode[] args = new ExpressionNode[] {receiver, operand};

    if (isSuperSend) {
      return MessageSendNode.createSuperSend(
          mgenc.getHolder().getSuperClass(), msg, args, coordWithL);
    } else if (msg.getString().equals("+") && operand instanceof IntegerLiteralNode) {
      IntegerLiteralNode lit = (IntegerLiteralNode) operand;
      if (lit.executeLong(null) == 1) {
        return IntIncrementNodeGen.create(receiver);
      }
    }
    return MessageSendNode.create(msg, args, coordWithL);
  }

  protected ExpressionNode keywordMessage(final MethodGenerationContext mgenc,
      final ExpressionNode receiver) throws ProgramDefinitionError {
    boolean isSuperSend = superSend;
    superSend = false;
    int coord = getStartIndex();
    List<ExpressionNode> arguments = new ArrayList<ExpressionNode>();
    StringBuilder kw = new StringBuilder();

    arguments.add(receiver);

    do {
      kw.append(keyword());
      arguments.add(formula(mgenc));
    } while (sym == Keyword);

    SSymbol msg = symbolFor(kw.toString());

    long coodWithL = getCoordWithLength(coord);

    ExpressionNode inlined = inlinableNodes.inline(msg, arguments, mgenc, coodWithL);
    if (inlined != null) {
      assert !isSuperSend;
      return inlined;
    }

    ExpressionNode[] args = arguments.toArray(new ExpressionNode[0]);
    if (isSuperSend) {
      return MessageSendNode.createSuperSend(
          mgenc.getHolder().getSuperClass(), msg, args, coodWithL);
    }
    return MessageSendNode.create(msg, args, coodWithL);
  }

  private ExpressionNode formula(final MethodGenerationContext mgenc)
      throws ProgramDefinitionError {
    ExpressionNode operand = binaryOperand(mgenc);

    while (sym == OperatorSequence || symIn(binaryOpSyms)) {
      operand = binaryMessage(mgenc, operand);
    }

    superSend = false;
    return operand;
  }

  private LiteralNode literal() throws ParseError {
    int coord = getStartIndex();
    switch (sym) {
      case Pound: {
        peekForNextSymbolFromLexerIfNecessary();
        Object value;
        if (nextSym == NewTerm) {
          value = literalArray();
        } else {
          value = literalSymbol();
        }
        return new GenericLiteralNode(value).initialize(getCoordWithLength(coord));
      }
      case STString:
        return new GenericLiteralNode(literalString()).initialize(getCoordWithLength(coord));
      default:
        boolean isNegative = isNegativeNumber();
        if (sym == Integer) {
          Object value = literalInteger(isNegative);
          if (value instanceof BigInteger) {
            return new GenericLiteralNode(value).initialize(getCoordWithLength(coord));
          } else {
            return new IntegerLiteralNode((long) value).initialize(getCoordWithLength(coord));
          }
        } else {
          if (sym != Double) {
            throw new ParseError("Unexpected symbol. Expected %(expected)s, but found "
                + "%(found)s", sym, this);
          }
          return new DoubleLiteralNode(literalDouble(isNegative)).initialize(
              getCoordWithLength(coord));
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
        return Globals.getGlobal(symbolFor(text));
      default:
        throw new ParseError("Could not parse literal array value", NONE, this);
    }
  }

  private ExpressionNode primary(final MethodGenerationContext mgenc)
      throws ProgramDefinitionError {
    switch (sym) {
      case Identifier:
      case Primitive: {
        int coord = getStartIndex();
        SSymbol v = variable();

        if (v == symSuper) {
          assert !superSend : "Since super is consumed directly, it should never be true here";
          superSend = true;
          // sends to super push self as the receiver
          v = symSelf;
        }

        return variableRead(mgenc, v, getCoordWithLength(coord));
      }
      case NewTerm: {
        return nestedTerm(mgenc);
      }
      case NewBlock: {
        int coord = getStartIndex();
        MethodGenerationContext bgenc = new MethodGenerationContext(mgenc.getHolder(), mgenc);

        ExpressionNode blockBody = nestedBlock(bgenc);

        SMethod blockMethod = (SMethod) bgenc.assemble(blockBody, lastMethodsCoord);
        mgenc.addEmbeddedBlockMethod(blockMethod);

        if (bgenc.requiresContext()) {
          return new BlockNodeWithContext(blockMethod).initialize(getCoordWithLength(coord));
        } else {
          return new BlockNode(blockMethod).initialize(getCoordWithLength(coord));
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
      operand = unaryMessage(mgenc, operand);
    }

    return operand;
  }
}
