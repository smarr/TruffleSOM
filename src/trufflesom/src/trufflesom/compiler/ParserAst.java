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
import static trufflesom.vm.SymbolTable.symNil;
import static trufflesom.vm.SymbolTable.symSelf;
import static trufflesom.vm.SymbolTable.symSuper;
import static trufflesom.vm.SymbolTable.symbolFor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.source.Source;

import trufflesom.bdt.basic.ProgramDefinitionError;
import trufflesom.bdt.inlining.InlinableNodes;
import trufflesom.bdt.tools.structure.StructuralProbe;
import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgumentReadNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.NonLocalArgumentReadNode;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.FieldNode;
import trufflesom.interpreter.nodes.FieldNode.FieldReadNode;
import trufflesom.interpreter.nodes.GlobalNode;
import trufflesom.interpreter.nodes.MessageSendNode;
import trufflesom.interpreter.nodes.SequenceNode;
import trufflesom.interpreter.nodes.literals.BlockNode;
import trufflesom.interpreter.nodes.literals.BlockNode.BlockNodeWithContext;
import trufflesom.interpreter.nodes.literals.DoubleLiteralNode;
import trufflesom.interpreter.nodes.literals.GenericLiteralNode;
import trufflesom.interpreter.nodes.literals.IntegerLiteralNode;
import trufflesom.interpreter.nodes.literals.LiteralNode;
import trufflesom.interpreter.supernodes.IntIncrementNodeGen;
import trufflesom.interpreter.supernodes.LocalFieldStringEqualsNode;
import trufflesom.interpreter.supernodes.NonLocalFieldStringEqualsNode;
import trufflesom.interpreter.supernodes.StringEqualsNodeGen;
import trufflesom.primitives.Primitives;
import trufflesom.vm.Globals;
import trufflesom.vm.NotYetImplementedException;
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
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structProbe) {
    return new MethodGenerationContext(cgenc, structProbe);
  }

  @Override
  protected ExpressionNode blockBody(final MethodGenerationContext mgenc,
      final boolean seenPeriod) throws ProgramDefinitionError {
    int coord = getStartIndex();
    List<ExpressionNode> expressions = new ArrayList<>();

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

    return new SequenceNode(expressions.toArray(new ExpressionNode[0])).initialize(
        getCoordWithLength(coord));
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

  protected ExpressionNode variableWrite(final MethodGenerationContext mgenc,
      final SSymbol variableName, final ExpressionNode exp, final long coord)
      throws ParseError {
    Variable variable = mgenc.getVariable(variableName);
    if (variable != null) {
      return mgenc.getLocalWriteNode(variable, exp, coord);
    }

    FieldNode fieldWrite = mgenc.getObjectFieldWrite(variableName, exp, coord);

    if (fieldWrite != null) {
      return fieldWrite;
    } else {
      throw new ParseError("Neither a variable nor a field found "
          + "in current scope that is named " + variableName + ". Arguments are read-only.",
          Symbol.NONE, this);
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
    SSymbol selector = unarySendSelector();

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
    SSymbol msg = binarySendSelector();
    ExpressionNode operand = binaryOperand(mgenc);

    long coordWithL = getCoordWithLength(coord);

    ExpressionNode[] args = new ExpressionNode[] {receiver, operand};

    if (isSuperSend) {
      return MessageSendNode.createSuperSend(
          mgenc.getHolder().getSuperClass(), msg, args, coordWithL);
    }

    String binSelector = msg.getString();

    if (binSelector.equals("=")) {
      if (operand instanceof GenericLiteralNode) {
        Object literal = operand.executeGeneric(null);
        if (literal instanceof String s) {
          if (receiver instanceof FieldReadNode fieldRead) {
            ExpressionNode self = fieldRead.getSelf();
            if (self instanceof LocalArgumentReadNode localSelf) {
              return new LocalFieldStringEqualsNode(fieldRead.getFieldIndex(),
                  localSelf.getArg(), s).initialize(coordWithL);
            } else if (self instanceof NonLocalArgumentReadNode arg) {
              return new NonLocalFieldStringEqualsNode(fieldRead.getFieldIndex(), arg.getArg(),
                  arg.getContextLevel(), s).initialize(coordWithL);
            } else {
              throw new NotYetImplementedException();
            }
          }

          return StringEqualsNodeGen.create(s, receiver).initialize(coordWithL);
        }
      }

      if (receiver instanceof GenericLiteralNode) {
        Object literal = receiver.executeGeneric(null);
        if (literal instanceof String s) {
          if (operand instanceof FieldReadNode fieldRead) {
            ExpressionNode self = fieldRead.getSelf();
            if (self instanceof LocalArgumentReadNode localSelf) {
              return new LocalFieldStringEqualsNode(fieldRead.getFieldIndex(),
                  localSelf.getArg(), s).initialize(coordWithL);
            } else if (self instanceof NonLocalArgumentReadNode arg) {
              return new NonLocalFieldStringEqualsNode(fieldRead.getFieldIndex(), arg.getArg(),
                  arg.getContextLevel(), s).initialize(coordWithL);
            } else {
              throw new NotYetImplementedException();
            }
          }

          return StringEqualsNodeGen.create(s, operand).initialize(coordWithL);
        }
      }
    }

    ExpressionNode inlined =
        inlinableNodes.inline(msg, args, mgenc, coordWithL);
    if (inlined != null) {
      assert !isSuperSend;
      return inlined;
    }

    if (msg.getString().equals("+") && operand instanceof IntegerLiteralNode lit) {
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
    List<ExpressionNode> arguments = new ArrayList<>();
    StringBuilder kw = new StringBuilder();

    arguments.add(receiver);

    do {
      kw.append(keywordInSend());
      arguments.add(formula(mgenc));
    } while (sym == Keyword);

    SSymbol msg = symbolFor(kw.toString());

    long coodWithL = getCoordWithLength(coord);

    ExpressionNode[] args = arguments.toArray(new ExpressionNode[0]);
    if (isSuperSend) {
      return MessageSendNode.createSuperSend(
          mgenc.getHolder().getSuperClass(), msg, args, coodWithL);
    }

    ExpressionNode inlined = inlinableNodes.inline(msg, args, mgenc, coodWithL);
    if (inlined != null) {
      assert !isSuperSend;
      return inlined;
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
    List<Object> literals = new ArrayList<>();
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
          return new BlockNodeWithContext(blockMethod,
              bgenc.accessesLocalsOfOuterScope).initialize(getCoordWithLength(coord));
        } else {
          return new BlockNode(blockMethod, bgenc.accessesLocalsOfOuterScope).initialize(
              getCoordWithLength(coord));
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
