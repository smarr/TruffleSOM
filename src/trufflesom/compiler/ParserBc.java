package trufflesom.compiler;

import static trufflesom.compiler.Symbol.Assign;
import static trufflesom.compiler.Symbol.Double;
import static trufflesom.compiler.Symbol.EndBlock;
import static trufflesom.compiler.Symbol.EndTerm;
import static trufflesom.compiler.Symbol.Exit;
import static trufflesom.compiler.Symbol.Identifier;
import static trufflesom.compiler.Symbol.Integer;
import static trufflesom.compiler.Symbol.Keyword;
import static trufflesom.compiler.Symbol.NewTerm;
import static trufflesom.compiler.Symbol.OperatorSequence;
import static trufflesom.compiler.Symbol.Period;
import static trufflesom.compiler.Symbol.Pound;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.source.Source;

import bd.basic.ProgramDefinitionError;
import bd.tools.structure.StructuralProbe;
import trufflesom.compiler.bc.BytecodeGenerator;
import trufflesom.compiler.bc.BytecodeMethodGenContext;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SSymbol;


public class ParserBc extends Parser<BytecodeMethodGenContext> {

  private final BytecodeGenerator bcGen;

  public ParserBc(final String content, final Source source,
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe,
      final Universe universe) {
    super(content, source, structuralProbe, universe);
    bcGen = new BytecodeGenerator();
  }

  @Override
  protected BytecodeMethodGenContext createMGenC(final ClassGenerationContext cgenc,
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe) {
    return new BytecodeMethodGenContext(cgenc, structuralProbe);
  }

  @Override
  protected ExpressionNode methodBlock(final BytecodeMethodGenContext mgenc)
      throws ProgramDefinitionError {
    super.methodBlock(mgenc);

    // if no return has been generated so far, we can be sure there was no .
    // terminating the last expression, so the last expression's value must
    // be popped off the stack and a ^self be generated
    if (!mgenc.isFinished()) {
      // with the new RETURN_SELF, we don't actually need the extra stack space
      // bcGen.emitPOP(mgenc);
      bcGen.emitRETURNSELF(mgenc);
      mgenc.markFinished();
    }

    return null;
  }

  @Override
  protected ExpressionNode blockBody(final BytecodeMethodGenContext mgenc,
      final boolean seenPeriod)
      throws ProgramDefinitionError {
    if (accept(Exit)) {
      result(mgenc);
    } else if (sym == EndBlock) {
      if (seenPeriod) {
        // a POP has been generated which must be elided (blocks always
        // return the value of the last expression, regardless of
        // whether it
        // was terminated with a . or not)
        mgenc.removeLastBytecode();
      }
      if (mgenc.isBlockMethod() && !mgenc.hasBytecodes()) {
        // if the block is empty, we need to return nil
        SSymbol nilSym = universe.symbolFor("nil");
        mgenc.addLiteralIfAbsent(nilSym, this);
        bcGen.emitPUSHGLOBAL(mgenc, nilSym);
      }
      bcGen.emitRETURNLOCAL(mgenc);
      mgenc.markFinished();
    } else if (sym == EndTerm) {
      // it does not matter whether a period has been seen,
      // as the end of the method has been found (EndTerm) -
      // so it is safe to emit a "return self"
      bcGen.emitRETURNSELF(mgenc);
      mgenc.markFinished();
    } else {
      expression(mgenc);
      if (accept(Period)) {
        bcGen.emitPOP(mgenc);
        blockBody(mgenc, true);
      }
    }

    return null;
  }

  @Override
  protected ExpressionNode result(final BytecodeMethodGenContext mgenc)
      throws ProgramDefinitionError {
    expression(mgenc);

    if (mgenc.isBlockMethod()) {
      mgenc.makeOuterCatchNonLocalReturn();
      bcGen.emitRETURNNONLOCAL(mgenc);
    } else {
      bcGen.emitRETURNLOCAL(mgenc);
    }

    mgenc.markFinished();
    accept(Period);

    return null;
  }

  @Override
  protected ExpressionNode assignation(final BytecodeMethodGenContext mgenc)
      throws ProgramDefinitionError {
    List<SSymbol> l = new ArrayList<>();

    assignments(mgenc, l);
    evaluation(mgenc);

    for (int i = 1; i <= l.size(); i++) {
      bcGen.emitDUP(mgenc);
    }
    for (SSymbol s : l) {
      genPopVariable(mgenc, s);
    }

    return null;
  }

  private void assignments(final BytecodeMethodGenContext mgenc, final List<SSymbol> l)
      throws ParseError {
    if (sym == Identifier) {
      // String varName = assignment();
      // SSymbol variable = universe.symbolFor(assignment());
      l.add(assignment());
      peekForNextSymbolFromLexer();
      if (nextSym == Assign) {
        assignments(mgenc, l);
      }
    }
  }

  @Override
  protected ExpressionNode evaluation(final BytecodeMethodGenContext mgenc)
      throws ProgramDefinitionError {
    boolean superSend = primary(mgenc);
    if (sym == Identifier || sym == Keyword || sym == OperatorSequence
        || symIn(binaryOpSyms)) {
      messages(mgenc, superSend);
    }

    return null;
  }

  private void messages(final BytecodeMethodGenContext mgenc, boolean superSend)
      throws ProgramDefinitionError {
    if (isIdentifier(sym)) {
      do {
        // only the first message in a sequence can be a super send
        unaryMessage(mgenc, superSend);
        superSend = false;
      } while (isIdentifier(sym));

      while (sym == OperatorSequence || symIn(binaryOpSyms)) {
        binaryMessage(mgenc, false);
      }

      if (sym == Keyword) {
        keywordMessage(mgenc, false);
      }
    } else if (sym == OperatorSequence || symIn(binaryOpSyms)) {
      do {
        // only the first message in a sequence can be a super send
        binaryMessage(mgenc, superSend);
        superSend = false;
      } while (sym == OperatorSequence || symIn(binaryOpSyms));

      if (sym == Keyword) {
        keywordMessage(mgenc, false);
      }
    } else {
      keywordMessage(mgenc, superSend);
    }
  }

  private void unaryMessage(final BytecodeMethodGenContext mgenc, final boolean superSend)
      throws ParseError {
    SSymbol msg = unarySelector();
    mgenc.addLiteralIfAbsent(msg, this);

    if (superSend) {
      bcGen.emitSUPERSEND(mgenc, msg);
    } else {
      bcGen.emitSEND(mgenc, msg);
    }
  }

  private void binaryMessage(final BytecodeMethodGenContext mgenc, final boolean superSend)
      throws ProgramDefinitionError {
    SSymbol msg = binarySelector();
    mgenc.addLiteralIfAbsent(msg, this);

    binaryOperand(mgenc);

    if (superSend) {
      bcGen.emitSUPERSEND(mgenc, msg);
    } else {
      bcGen.emitSEND(mgenc, msg);
    }
  }

  private void keywordMessage(final BytecodeMethodGenContext mgenc, final boolean superSend)
      throws ProgramDefinitionError {
    StringBuilder kw = new StringBuilder();
    do {
      kw.append(keyword());
      formula(mgenc);
    } while (sym == Keyword);

    SSymbol msg = universe.symbolFor(kw.toString());

    mgenc.addLiteralIfAbsent(msg, this);

    if (superSend) {
      bcGen.emitSUPERSEND(mgenc, msg);
    } else {
      bcGen.emitSEND(mgenc, msg);
    }
  }

  private void formula(final BytecodeMethodGenContext mgenc) throws ProgramDefinitionError {
    boolean superSend = binaryOperand(mgenc);

    // only the first message in a sequence can be a super send
    if (sym == OperatorSequence || symIn(binaryOpSyms)) {
      binaryMessage(mgenc, superSend);
    }

    while (sym == OperatorSequence || symIn(binaryOpSyms)) {
      binaryMessage(mgenc, false);
    }
  }

  private void literal(final BytecodeMethodGenContext mgenc) throws ParseError {
    switch (sym) {
      case Pound: {
        peekForNextSymbolFromLexerIfNecessary();
        if (nextSym == NewTerm) {
          literalArray(mgenc);
        } else {
          SSymbol sym = literalSymbol();
          mgenc.addLiteralIfAbsent(sym, this);
          bcGen.emitPUSHCONSTANT(mgenc, sym);
        }
        break;
      }
      case STString: {
        String str = literalString();

        mgenc.addLiteralIfAbsent(str, this);
        bcGen.emitPUSHCONSTANT(mgenc, str);
        break;
      }
      default: {
        literalNumber(mgenc);
        break;
      }
    }
  }

  private void literalNumber(final BytecodeMethodGenContext mgenc) throws ParseError {
    boolean isNegative = isNegativeNumber();

    Object lit;
    if (sym == Integer) {
      lit = literalInteger(isNegative);
    } else {
      assert sym == Double;
      lit = literalDouble(isNegative);
    }

    mgenc.addLiteralIfAbsent(lit, this);
    bcGen.emitPUSHCONSTANT(mgenc, lit);
  }

  private void literalArray(final BytecodeMethodGenContext mgenc) throws ParseError {
    expect(Pound);
    expect(NewTerm);

    SSymbol arrayClassName = universe.symbolFor("Array");
    SSymbol arraySizePlaceholder = universe.symbolFor("ArraySizeLiteralPlaceholder");
    SSymbol newMessage = universe.symbolFor("new:");
    SSymbol atPutMessage = universe.symbolFor("at:put:");

    mgenc.addLiteralIfAbsent(arrayClassName, this);
    mgenc.addLiteralIfAbsent(newMessage, this);
    mgenc.addLiteralIfAbsent(atPutMessage, this);
    final byte arraySizeLiteralIndex = mgenc.addLiteral(arraySizePlaceholder, this);

    // create empty array
    bcGen.emitPUSHGLOBAL(mgenc, arrayClassName);
    bcGen.emitPUSHCONSTANT(mgenc, arraySizeLiteralIndex);
    bcGen.emitSEND(mgenc, newMessage);

    long i = 1;

    while (sym != EndTerm) {
      bcGen.emitDUP(mgenc); // dup the array for having it on the stack after the #at:put:

      mgenc.addLiteralIfAbsent(i, this);
      bcGen.emitPUSHCONSTANT(mgenc, i);
      literal(mgenc);
      bcGen.emitSEND(mgenc, atPutMessage);
      bcGen.emitPOP(mgenc);
      i += 1;
    }

    // replace the placeholder with the actual array size
    Object size = i - 1;
    mgenc.updateLiteral(arraySizePlaceholder, arraySizeLiteralIndex, size);
    expect(EndTerm);
  }

  private boolean primary(final BytecodeMethodGenContext mgenc)
      throws ProgramDefinitionError {
    boolean superSend = false;
    switch (sym) {
      case Identifier: {
        SSymbol v = variable();

        if (v == universe.symSuper) {
          superSend = true;
          // sends to super push self as the receiver
          v = universe.symSelf;
        }

        genPushVariable(mgenc, v);
        break;
      }
      case NewTerm:
        nestedTerm(mgenc);
        break;
      case NewBlock: {
        BytecodeMethodGenContext bgenc =
            new BytecodeMethodGenContext(mgenc.getHolder(), mgenc);
        nestedBlock(bgenc);

        SMethod blockMethod = (SMethod) bgenc.assemble(
            null, lastMethodsSourceSection, lastMethodsSourceSection);
        mgenc.addEmbeddedBlockMethod(blockMethod);
        mgenc.addLiteral(blockMethod, this);
        bcGen.emitPUSHBLOCK(mgenc, blockMethod);
        break;
      }
      default:
        literal(mgenc);
        break;
    }
    return superSend;
  }

  private boolean binaryOperand(final BytecodeMethodGenContext mgenc)
      throws ProgramDefinitionError {
    boolean superSend = primary(mgenc);

    while (sym == Identifier) {
      unaryMessage(mgenc, superSend);
      superSend = false;
    }

    return superSend;
  }

  @Override
  protected ExpressionNode nestedBlock(final BytecodeMethodGenContext mgenc)
      throws ProgramDefinitionError {
    super.nestedBlock(mgenc);

    // if no return has been generated, we can be sure that the last expression in the block
    // was not terminated by ., and can generate a return
    if (!mgenc.isFinished()) {
      if (!mgenc.hasBytecodes()) {
        // if the block is empty, we need to return nil
        SSymbol nilSym = universe.symbolFor("nil");
        mgenc.addLiteralIfAbsent(nilSym, this);
        bcGen.emitPUSHGLOBAL(mgenc, nilSym);
      }
      bcGen.emitRETURNLOCAL(mgenc);
      mgenc.markFinished();
    }

    return null;
  }

  private void genPushVariable(final BytecodeMethodGenContext mgenc, final SSymbol var)
      throws ParseError {
    // The purpose of this function is to find out whether the variable to be
    // pushed on the stack is a local variable, argument, or object field.
    // This is done by examining all available lexical contexts, starting with
    // the innermost (i.e., the one represented by mgenc).

    Variable variable = mgenc.getVariable(var);
    if (variable != null) {
      variable.emitPush(bcGen, mgenc);
    } else {
      if (mgenc.hasField(var)) {
        bcGen.emitPUSHFIELD(mgenc, var);
      } else {
        mgenc.addLiteralIfAbsent(var, this);
        bcGen.emitPUSHGLOBAL(mgenc, var);
      }
    }
  }

  private void genPopVariable(final BytecodeMethodGenContext mgenc, final SSymbol var)
      throws ParseError {
    // The purpose of this function is to find out whether the variable to be
    // popped off the stack is a local variable, argument, or object field.
    // This is done by examining all available lexical contexts, starting with
    // the innermost (i.e., the one represented by mgenc).

    Variable variable = mgenc.getVariable(var);
    if (variable != null) {
      variable.emitPop(bcGen, mgenc);
    } else {
      if (!mgenc.hasField(var)) {
        throw new ParseError("Trying to write to field with the name '" + var.getString()
            + "', but field does not seem exist in class.", Symbol.NONE, this);
      }
      bcGen.emitPOPFIELD(mgenc, var);
    }
  }
}
