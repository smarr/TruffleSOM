package trufflesom.compiler;

import static trufflesom.compiler.Symbol.Assign;
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
import static trufflesom.compiler.bc.BytecodeGenerator.emitDEC;
import static trufflesom.compiler.bc.BytecodeGenerator.emitDUP;
import static trufflesom.compiler.bc.BytecodeGenerator.emitINC;
import static trufflesom.compiler.bc.BytecodeGenerator.emitPOP;
import static trufflesom.compiler.bc.BytecodeGenerator.emitPOPFIELD;
import static trufflesom.compiler.bc.BytecodeGenerator.emitPUSHBLOCK;
import static trufflesom.compiler.bc.BytecodeGenerator.emitPUSHCONSTANT;
import static trufflesom.compiler.bc.BytecodeGenerator.emitPUSHFIELD;
import static trufflesom.compiler.bc.BytecodeGenerator.emitPUSHGLOBAL;
import static trufflesom.compiler.bc.BytecodeGenerator.emitRETURNLOCAL;
import static trufflesom.compiler.bc.BytecodeGenerator.emitRETURNNONLOCAL;
import static trufflesom.compiler.bc.BytecodeGenerator.emitRETURNSELF;
import static trufflesom.compiler.bc.BytecodeGenerator.emitSEND;
import static trufflesom.compiler.bc.BytecodeGenerator.emitSUPERSEND;
import static trufflesom.vm.SymbolTable.symArray;
import static trufflesom.vm.SymbolTable.symArraySizePlaceholder;
import static trufflesom.vm.SymbolTable.symAtPutMsg;
import static trufflesom.vm.SymbolTable.symMinus;
import static trufflesom.vm.SymbolTable.symNewMsg;
import static trufflesom.vm.SymbolTable.symPlus;
import static trufflesom.vm.SymbolTable.symSelf;
import static trufflesom.vm.SymbolTable.symSuper;
import static trufflesom.vm.SymbolTable.symbolFor;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.source.Source;

import trufflesom.bdt.basic.ProgramDefinitionError;
import trufflesom.bdt.tools.structure.StructuralProbe;
import trufflesom.compiler.bc.BytecodeMethodGenContext;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SSymbol;


public class ParserBc extends Parser<BytecodeMethodGenContext> {

  public ParserBc(final String content, final Source source,
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe) {
    super(content, source, structuralProbe);
  }

  @Override
  protected BytecodeMethodGenContext createMGenC(final ClassGenerationContext cgenc,
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structProbe) {
    return new BytecodeMethodGenContext(cgenc, structProbe);
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
      // emitPOP(mgenc);
      emitRETURNSELF(mgenc);
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
        // whether it was terminated with a . or not)
        mgenc.removeLastPopForBlockLocalReturn();
      }
      if (mgenc.isBlockMethod() && !mgenc.hasBytecodes()) {
        // if the block is empty, we need to return nil
        emitPUSHCONSTANT(mgenc, Nil.nilObject, this);
      }
      emitRETURNLOCAL(mgenc);
      mgenc.markFinished();
    } else if (sym == EndTerm) {
      // it does not matter whether a period has been seen,
      // as the end of the method has been found (EndTerm) -
      // so it is safe to emit a "return self"
      emitRETURNSELF(mgenc);
      mgenc.markFinished();
    } else {
      expression(mgenc);
      if (accept(Period)) {
        emitPOP(mgenc);
        blockBody(mgenc, true);
      }
    }

    return null;
  }

  @Override
  protected ExpressionNode result(final BytecodeMethodGenContext mgenc)
      throws ProgramDefinitionError {
    // try to parse a `^ self` to emit RETURN_SELF
    if (!mgenc.isBlockMethod() && sym == Identifier) {
      if (text.equals("self")) {
        peekForNextSymbolFromLexerIfNecessary();
        if (nextSym == Period || nextSym == EndTerm) {
          expect(Identifier);

          emitRETURNSELF(mgenc);
          mgenc.markFinished();

          accept(Period);
          return null;
        }
      }
    }

    expression(mgenc);

    if (mgenc.isBlockMethod()) {
      mgenc.makeOuterCatchNonLocalReturn();
      emitRETURNNONLOCAL(mgenc);
    } else {
      emitRETURNLOCAL(mgenc);
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
      emitDUP(mgenc);
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
    primary(mgenc);
    if (sym == Identifier || sym == Keyword || sym == OperatorSequence
        || symIn(binaryOpSyms)) {
      messages(mgenc);
    }

    superSend = false;
    return null;
  }

  private void messages(final BytecodeMethodGenContext mgenc) throws ProgramDefinitionError {
    if (isIdentifier(sym)) {
      do {
        // only the first message in a sequence can be a super send
        unaryMessage(mgenc);
        superSend = false;
      } while (isIdentifier(sym));

      while (sym == OperatorSequence || symIn(binaryOpSyms)) {
        binaryMessage(mgenc);
      }

      if (sym == Keyword) {
        keywordMessage(mgenc);
      }
    } else if (sym == OperatorSequence || symIn(binaryOpSyms)) {
      do {
        // only the first message in a sequence can be a super send
        binaryMessage(mgenc);
        superSend = false;
      } while (sym == OperatorSequence || symIn(binaryOpSyms));

      if (sym == Keyword) {
        keywordMessage(mgenc);
      }
    } else {
      keywordMessage(mgenc);
    }
  }

  protected void unaryMessage(final BytecodeMethodGenContext mgenc)
      throws ParseError {
    boolean isSuperSend = superSend;
    superSend = false;

    SSymbol msg = unarySendSelector();
    if (isSuperSend) {
      emitSUPERSEND(mgenc, msg, this);
    } else {
      emitSEND(mgenc, msg, this);
    }
  }

  private void binaryMessage(final BytecodeMethodGenContext mgenc)
      throws ProgramDefinitionError {
    boolean isSuperSend = superSend;
    superSend = false;

    SSymbol msg = binarySendSelector();

    boolean isPossibleIncOrDec = msg == symPlus || msg == symMinus;
    if (isPossibleIncOrDec) {
      if (sym == Integer && text.equals("1")) {
        expect(Integer);
        if (msg == symPlus) {
          emitINC(mgenc);
        } else {
          emitDEC(mgenc);
        }
        return;
      }
    }

    binaryOperand(mgenc);

    if (isSuperSend) {
      emitSUPERSEND(mgenc, msg, this);
    } else {
      if ((msg.getString().equals("||") && mgenc.inlineAndOr(this, true))
          || (msg.getString().equals("&&") && mgenc.inlineAndOr(this, false))) {
        return;
      }
      emitSEND(mgenc, msg, this);
    }
  }

  private void keywordMessage(final BytecodeMethodGenContext mgenc)
      throws ProgramDefinitionError {
    boolean isSuperSend = superSend;
    superSend = false;

    StringBuilder kw = new StringBuilder();
    do {
      kw.append(keywordInSend());
      formula(mgenc);
    } while (sym == Keyword);

    String kwStr = kw.toString();

    if (!isSuperSend) {
      if (("ifTrue:".equals(kwStr) && mgenc.inlineIfTrueOrIfFalse(this, true)) ||
          ("ifFalse:".equals(kwStr) && mgenc.inlineIfTrueOrIfFalse(this, false)) ||
          ("ifTrue:ifFalse:".equals(kwStr) && mgenc.inlineIfTrueIfFalse(this, true)) ||
          ("ifFalse:ifTrue:".equals(kwStr) && mgenc.inlineIfTrueIfFalse(this, false)) ||
          ("whileTrue:".equals(kwStr) && mgenc.inlineWhileTrueOrFalse(this, true)) ||
          ("whileFalse:".equals(kwStr) && mgenc.inlineWhileTrueOrFalse(this, false)) ||
          ("or:".equals(kwStr) && mgenc.inlineAndOr(this, true)) ||
          ("and:".equals(kwStr) && mgenc.inlineAndOr(this, false))) {
        // all done
        return;
      }
    }

    SSymbol msg = symbolFor(kwStr);

    if (isSuperSend) {
      emitSUPERSEND(mgenc, msg, this);
    } else {
      emitSEND(mgenc, msg, this);
    }
  }

  private void formula(final BytecodeMethodGenContext mgenc) throws ProgramDefinitionError {
    binaryOperand(mgenc);

    // only the first message in a sequence can be a super send
    if (sym == OperatorSequence || symIn(binaryOpSyms)) {
      binaryMessage(mgenc);
    }

    while (sym == OperatorSequence || symIn(binaryOpSyms)) {
      binaryMessage(mgenc);
    }

    superSend = false;
  }

  private void literal(final BytecodeMethodGenContext mgenc) throws ParseError {
    switch (sym) {
      case Pound: {
        peekForNextSymbolFromLexerIfNecessary();
        if (nextSym == NewTerm) {
          literalArray(mgenc);
        } else {
          SSymbol s = literalSymbol();
          emitPUSHCONSTANT(mgenc, s, this);
        }
        break;
      }
      case STString: {
        String str = literalString();
        emitPUSHCONSTANT(mgenc, str, this);
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
      lit = literalDouble(isNegative);
    }

    emitPUSHCONSTANT(mgenc, lit, this);
  }

  private void literalArray(final BytecodeMethodGenContext mgenc) throws ParseError {
    expect(Pound);
    expect(NewTerm);

    final byte arraySizeLiteralIndex = mgenc.addLiteral(symArraySizePlaceholder, this);

    // create empty array
    emitPUSHGLOBAL(mgenc, symArray, this);
    emitPUSHCONSTANT(mgenc, arraySizeLiteralIndex);
    emitSEND(mgenc, symNewMsg, this);

    long i = 1;

    while (sym != EndTerm) {
      emitDUP(mgenc); // dup the array for having it on the stack after the #at:put:

      emitPUSHCONSTANT(mgenc, i, this);
      literal(mgenc);
      emitSEND(mgenc, symAtPutMsg, this);
      emitPOP(mgenc);
      i += 1;
    }

    // replace the placeholder with the actual array size
    Object size = i - 1;
    mgenc.updateLiteral(symArraySizePlaceholder, arraySizeLiteralIndex, size);
    expect(EndTerm);
  }

  private void primary(final BytecodeMethodGenContext mgenc) throws ProgramDefinitionError {
    switch (sym) {
      case Identifier:
      case Primitive: {
        SSymbol v = variable();

        if (v == symSuper) {
          superSend = true;
          // sends to super push self as the receiver
          v = symSelf;
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

        SMethod blockMethod = (SMethod) bgenc.assemble(null, lastMethodsCoord);
        mgenc.addEmbeddedBlockMethod(blockMethod);
        mgenc.addLiteral(blockMethod, this);
        emitPUSHBLOCK(mgenc, blockMethod, bgenc.requiresContext());
        break;
      }
      default:
        literal(mgenc);
        break;
    }
  }

  private void binaryOperand(final BytecodeMethodGenContext mgenc)
      throws ProgramDefinitionError {
    primary(mgenc);

    while (sym == Identifier) {
      unaryMessage(mgenc);
    }
  }

  @Override
  public ExpressionNode nestedBlock(final BytecodeMethodGenContext mgenc)
      throws ProgramDefinitionError {
    super.nestedBlock(mgenc);

    // if no return has been generated, we can be sure that the last expression in the block
    // was not terminated by ., and can generate a return
    if (!mgenc.isFinished()) {
      if (!mgenc.hasBytecodes()) {
        // if the block is empty, we need to return nil
        emitPUSHCONSTANT(mgenc, Nil.nilObject, this);
      }
      emitRETURNLOCAL(mgenc);
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
      variable.emitPush(mgenc);
    } else {
      if (mgenc.hasField(var)) {
        emitPUSHFIELD(mgenc, var);
      } else {
        emitPUSHGLOBAL(mgenc, var, this);
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
      variable.emitPop(mgenc);
    } else {
      if (!mgenc.hasField(var)) {
        throw new ParseError("Trying to write to field with the name '" + var.getString()
            + "', but field does not seem exist in class.", Symbol.NONE, this);
      }
      emitPOPFIELD(mgenc, var);
    }
  }
}
