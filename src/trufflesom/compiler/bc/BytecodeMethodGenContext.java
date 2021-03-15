package trufflesom.compiler.bc;

import static trufflesom.compiler.bc.BytecodeGenerator.emitJumpOnFalseWithDummyOffset;
import static trufflesom.compiler.bc.BytecodeGenerator.emitJumpOnTrueWithDummyOffset;
import static trufflesom.compiler.bc.BytecodeGenerator.emitJumpWithDummyOffset;
import static trufflesom.interpreter.bc.Bytecodes.DEC;
import static trufflesom.interpreter.bc.Bytecodes.DUP;
import static trufflesom.interpreter.bc.Bytecodes.HALT;
import static trufflesom.interpreter.bc.Bytecodes.INC;
import static trufflesom.interpreter.bc.Bytecodes.INC_FIELD;
import static trufflesom.interpreter.bc.Bytecodes.INC_FIELD_PUSH;
import static trufflesom.interpreter.bc.Bytecodes.INVALID;
import static trufflesom.interpreter.bc.Bytecodes.JUMP;
import static trufflesom.interpreter.bc.Bytecodes.JUMP_ON_FALSE_POP;
import static trufflesom.interpreter.bc.Bytecodes.JUMP_ON_FALSE_TOP_NIL;
import static trufflesom.interpreter.bc.Bytecodes.JUMP_ON_TRUE_POP;
import static trufflesom.interpreter.bc.Bytecodes.JUMP_ON_TRUE_TOP_NIL;
import static trufflesom.interpreter.bc.Bytecodes.POP;
import static trufflesom.interpreter.bc.Bytecodes.POP_ARGUMENT;
import static trufflesom.interpreter.bc.Bytecodes.POP_FIELD;
import static trufflesom.interpreter.bc.Bytecodes.POP_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_ARGUMENT;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_BLOCK;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_CONSTANT;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_FIELD;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_GLOBAL;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.RETURN_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.RETURN_NON_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.RETURN_SELF;
import static trufflesom.interpreter.bc.Bytecodes.SEND;
import static trufflesom.interpreter.bc.Bytecodes.SUPER_SEND;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.source.SourceSection;

import bd.tools.structure.StructuralProbe;
import trufflesom.compiler.ClassGenerationContext;
import trufflesom.compiler.Field;
import trufflesom.compiler.MethodGenerationContext;
import trufflesom.compiler.Parser.ParseError;
import trufflesom.compiler.ParserBc;
import trufflesom.compiler.Symbol;
import trufflesom.compiler.Variable;
import trufflesom.compiler.Variable.Argument;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.bc.Bytecodes;
import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgumentReadNode;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.FieldNode.FieldReadNode;
import trufflesom.interpreter.nodes.FieldNode.FieldWriteNode;
import trufflesom.interpreter.nodes.GlobalNode;
import trufflesom.interpreter.nodes.bc.BytecodeLoopNode;
import trufflesom.interpreter.nodes.literals.LiteralNode;
import trufflesom.vm.NotYetImplementedException;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SAbstractObject;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SSymbol;


public class BytecodeMethodGenContext extends MethodGenerationContext {

  private final List<Object>                  literals;
  private final LinkedHashMap<SSymbol, Local> localAndOuterVars;

  private final ArrayList<Byte> bytecode;
  private final byte[]          last4Bytecodes;

  private boolean finished;
  private boolean isCurrentlyInliningBlock = false;

  public BytecodeMethodGenContext(final ClassGenerationContext holderGenc,
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe) {
    this(holderGenc, null, holderGenc.getUniverse(), false, structuralProbe);
  }

  public BytecodeMethodGenContext(final Universe universe,
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe) {
    this(null, null, universe, false, structuralProbe);
  }

  public BytecodeMethodGenContext(final ClassGenerationContext holderGenc,
      final MethodGenerationContext outerGenc) {
    this(holderGenc, outerGenc, holderGenc.getUniverse(), true, outerGenc.structuralProbe);
  }

  private BytecodeMethodGenContext(final ClassGenerationContext holderGenc,
      final MethodGenerationContext outerGenc, final Universe universe,
      final boolean isBlockMethod,
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe) {
    super(holderGenc, outerGenc, universe, isBlockMethod, structuralProbe);
    literals = new ArrayList<>();
    bytecode = new ArrayList<>();
    localAndOuterVars = new LinkedHashMap<>();
    last4Bytecodes = new byte[4];
  }

  public byte getMaxContextLevel() {
    if (outerGenc != null) {
      return (byte) (1 + ((BytecodeMethodGenContext) outerGenc).getMaxContextLevel());
    }
    return 0;
  }

  public boolean hasField(final SSymbol fieldName) {
    return holderGenc.hasField(fieldName);
  }

  public byte getFieldIndex(final SSymbol fieldName) {
    return holderGenc.getFieldIndex(fieldName);
  }

  public byte findLiteralIndex(final Object lit) {
    return (byte) literals.indexOf(lit);
  }

  public void addBytecode(final byte code) {
    bytecode.add(code);
    last4Bytecodes[0] = last4Bytecodes[1];
    last4Bytecodes[1] = last4Bytecodes[2];
    last4Bytecodes[2] = last4Bytecodes[3];
    last4Bytecodes[3] = code;
  }

  public int addBytecodeArgumentAndGetIndex(final byte code) {
    int idx = bytecode.size();
    bytecode.add(code);
    return idx;
  }

  public void addBytecodeArgument(final byte code) {
    bytecode.add(code);
  }

  public void patchJumpOffsetToPointToNextInstruction(final int idxOfOffset) {
    int instructionStart = idxOfOffset - 1;
    byte bytecodeBeforeOffset = bytecode.get(instructionStart);
    assert bytecodeBeforeOffset == JUMP_ON_TRUE_TOP_NIL ||
        bytecodeBeforeOffset == JUMP_ON_FALSE_TOP_NIL ||
        bytecodeBeforeOffset == JUMP_ON_TRUE_POP ||
        bytecodeBeforeOffset == JUMP_ON_FALSE_POP ||
        bytecodeBeforeOffset == JUMP : "Expected to patch a JUMP instruction, but got bc: "
            + bytecodeBeforeOffset;
    int jumpOffset = bytecode.size() - instructionStart;

    assert jumpOffset > 0
        && jumpOffset <= 0xff : "The jumpOffset for the JUMP* bytecode is too large or small. jumpOffset="
            + jumpOffset;

    bytecode.set(idxOfOffset, (byte) jumpOffset);
  }

  @Override
  public boolean isFinished() {
    return finished;
  }

  @Override
  public void markFinished() {
    this.finished = true;
  }

  public boolean hasBytecodes() {
    return !bytecode.isEmpty();
  }

  /**
   * Remove the last POP bytecode, if it's there. It may have been optimized out by
   * {@link #optimizeDupPopPopSequence()}.
   */
  public void removeLastPopForBlockLocalReturn() {
    if (last4Bytecodes[3] == POP) {
      int idx = bytecode.size() - 1;
      bytecode.remove(idx);
    } else if ((last4Bytecodes[3] == POP_FIELD || last4Bytecodes[3] == POP_LOCAL)
        && last4Bytecodes[2] == -1) {
      // we just removed the DUP and didn't emit the POP using optimizeDupPopPopSequence()
      // so, to make blocks work, we need to reintroduce the DUP
      assert Bytecodes.getBytecodeLength(POP_LOCAL) == Bytecodes.getBytecodeLength(POP_FIELD);
      assert Bytecodes.getBytecodeLength(POP_LOCAL) == 3;
      assert bytecode.get(bytecode.size() - 3) == POP_LOCAL
          || bytecode.get(bytecode.size() - 3) == POP_FIELD;
      bytecode.add(bytecode.size() - 3, DUP);
    } else if (last4Bytecodes[3] == INC_FIELD) {
      // we optimized the sequence to an INC_FIELD, which doesn't modify the stack
      // but since we need the value to return it from the block, we need to push it.
      last4Bytecodes[3] = INC_FIELD_PUSH;
      assert Bytecodes.getBytecodeLength(INC_FIELD) == 3;
      assert Bytecodes.getBytecodeLength(INC_FIELD) == Bytecodes.getBytecodeLength(
          INC_FIELD_PUSH);
      bytecode.set(bytecode.size() - 3, INC_FIELD_PUSH);
    }
  }

  public boolean addLiteralIfAbsent(final Object lit, final ParserBc parser)
      throws ParseError {
    if (literals.contains(lit)) {
      return false;
    }

    addLiteral(lit, parser);
    return true;
  }

  public byte addLiteral(final Object lit, final ParserBc parser) throws ParseError {
    int i = literals.size();
    if (i > Byte.MAX_VALUE) {
      String methodSignature = holderGenc.getName().getString() + ">>" + signature;
      throw new ParseError(
          "The method " + methodSignature + " has more than the supported " +
              Byte.MAX_VALUE
              + " literal values. Please split the method. The literal to be added is: " + lit,
          Symbol.NONE, parser);
    }
    literals.add(lit);
    return (byte) i;
  }

  public void updateLiteral(final SAbstractObject oldVal, final byte index,
      final Object newVal) {
    assert literals.get(index) == oldVal;
    literals.set(index, newVal);
  }

  private byte getPositionIn(final Local local, final LinkedHashMap<SSymbol, Local> map) {
    byte i = 0;
    for (Local l : map.values()) {
      if (l.equals(local)) {
        return i;
      }
      i += 1;
    }
    return -1;
  }

  /**
   * Record the access, and also manage the tracking of outer access.
   */
  public byte getLocalIndex(final Local local, final int contextLevel) {
    byte pos = getPositionIn(local, localAndOuterVars);
    if (pos >= 0) {
      return pos;
    }

    // Don't have it yet, so, need to add it. Must be an outer,
    int size = localAndOuterVars.size();
    assert !localAndOuterVars.containsKey(local.getName());
    localAndOuterVars.put(local.getName(), local);
    assert getPositionIn(local, localAndOuterVars) == size;

    return (byte) size;
  }

  @Override
  public Local addLocal(final SSymbol local, final SourceSection source) {
    Local l = super.addLocal(local, source);
    localAndOuterVars.put(local, l);
    return l;
  }

  @Override
  public void addLocal(final Local l, final SSymbol name) {
    super.addLocal(l, name);
    assert !localAndOuterVars.containsKey(name);
    localAndOuterVars.put(name, l);
  }

  private BytecodeLoopNode constructBytecodeBody(final SourceSection sourceSection) {
    byte[] bytecodes = new byte[bytecode.size()];
    int i = 0;
    for (byte bc : bytecode) {
      bytecodes[i] = bc;
      i += 1;
    }

    Object[] literalsArr = literals.toArray();
    FrameSlot[] localsAndOuters = new FrameSlot[localAndOuterVars.size()];

    i = 0;
    for (Local l : localAndOuterVars.values()) {
      localsAndOuters[i] = l.getSlot();
      i += 1;
    }

    FrameSlot frameOnStackMarker =
        throwsNonLocalReturn ? getFrameOnStackMarker(sourceSection).getSlot() : null;

    return new BytecodeLoopNode(
        bytecodes, locals.size(), localsAndOuters, literalsArr, computeStackDepth(),
        frameOnStackMarker, universe);
  }

  private ExpressionNode constructTrivialBody() {
    ExpressionNode expr = optimizeLiteralReturn();

    if (expr == null) {
      expr = optimizeGlobalReturn();
    }

    if (expr == null) {
      expr = optimizeFieldGetter();
    }

    if (expr == null) {
      expr = optimizeFieldSetter();
    }

    return expr;
  }

  @Override
  protected SMethod assembleMethod(final ExpressionNode unused,
      final SourceSection sourceSection, final SourceSection fullSourceSection) {
    ExpressionNode body = constructTrivialBody();
    if (body == null) {
      body = constructBytecodeBody(sourceSection);
    }

    body.initialize(sourceSection);
    return super.assembleMethod(body, sourceSection, fullSourceSection);
  }

  /**
   * Invalidate last4Bytecodes to avoid optimizations which mess with branches.
   */
  private void resetLastBytecodeBuffer() {
    last4Bytecodes[0] = last4Bytecodes[1] = last4Bytecodes[2] = last4Bytecodes[3] = -1;
  }

  private byte lastBytecodeIs(final int idxFromEnd, final byte candidate) {
    byte actual = last4Bytecodes[last4Bytecodes.length - 1 - idxFromEnd];

    if (candidate == actual) {
      return actual;
    }

    return INVALID;
  }

  private byte lastBytecodeIsOneOf(final int idxFromEnd, final byte[] candidates) {
    byte actual = last4Bytecodes[last4Bytecodes.length - 1 - idxFromEnd];

    if (actual == INVALID) {
      return INVALID;
    }

    for (byte c : candidates) {
      if (c == actual) {
        return actual;
      }
    }

    return INVALID;
  }

  private void removeLastBytecodeAt(final int idxFromEnd) {
    final int bcOffset = getOffsetOfLastBytecode(idxFromEnd);

    byte bcToBeRemoved = last4Bytecodes[last4Bytecodes.length - 1 - idxFromEnd];
    int bcLength = Bytecodes.getBytecodeLength(bcToBeRemoved);

    for (int i = 0; i < bcLength; i += 1) {
      bytecode.remove(bcOffset);
    }
  }

  private void removeLastBytecodes(final int n) {
    for (int lastBytecode = 0; lastBytecode < n; lastBytecode += 1) {
      byte bcToBeRemoved = last4Bytecodes[last4Bytecodes.length - 1 - lastBytecode];
      int bcLength = Bytecodes.getBytecodeLength(bcToBeRemoved);

      for (int i = 0; i < bcLength; i += 1) {
        bytecode.remove(bytecode.size() - 1);
      }
    }
  }

  private int getOffsetOfLastBytecode(final int idxFromEnd) {
    int bcOffset = bytecode.size();
    for (int i = 0; i <= idxFromEnd; i += 1) {
      byte actual = last4Bytecodes[last4Bytecodes.length - 1 - i];
      if (actual == INVALID) {
        throw new IllegalStateException("The requested bytecode is not a valid one");
      }

      bcOffset -= Bytecodes.getBytecodeLength(actual);
    }
    return bcOffset;
  }

  private static final byte[] DUP_BYTECODES        = new byte[] {DUP};
  private static final byte[] INC_BYTECODES        = new byte[] {INC};
  private static final byte[] PUSH_BLOCK_BYTECODES = new byte[] {PUSH_BLOCK};

  private static final byte[] POP_LOCAL_FIELD_BYTECODES = new byte[] {
      POP_LOCAL,
      POP_FIELD};

  private static final byte[] PUSH_FIELD_BYTECODES = new byte[] {
      PUSH_FIELD};

  private static final byte[] POP_FIELD_BYTECODES = new byte[] {
      POP_FIELD};

  private LiteralNode optimizeLiteralReturn() {
    final byte pushCandidate = lastBytecodeIs(1, PUSH_CONSTANT);
    if (pushCandidate == INVALID) {
      return null;
    }

    final byte returnCandidate = lastBytecodeIs(0, RETURN_LOCAL);
    if (returnCandidate == INVALID) {
      return null;
    }

    if (bytecode.size() != (Bytecodes.getBytecodeLength(returnCandidate)
        + Bytecodes.getBytecodeLength(pushCandidate))) {
      return null;
    }

    byte constantIdx = getIndex(1);
    Object literal = literals.get(constantIdx);
    return LiteralNode.create(literal);
  }

  private GlobalNode optimizeGlobalReturn() {
    final byte pushCandidate = lastBytecodeIs(1, PUSH_GLOBAL);
    if (pushCandidate == INVALID) {
      return null;
    }

    final byte returnCandidate = lastBytecodeIs(0, RETURN_LOCAL);
    if (returnCandidate == INVALID) {
      return null;
    }

    if (bytecode.size() != (Bytecodes.getBytecodeLength(returnCandidate)
        + Bytecodes.getBytecodeLength(pushCandidate))) {
      return null;
    }

    byte constantIdx = getIndex(1);
    SSymbol literal = (SSymbol) literals.get(constantIdx);
    return GlobalNode.create(literal, universe);
  }

  private FieldReadNode optimizeFieldGetter() {
    if (isBlockMethod()) {
      return null;
    }

    final byte pushCandidate = lastBytecodeIsOneOf(1, PUSH_FIELD_BYTECODES);
    if (pushCandidate == INVALID) {
      return null;
    }

    final byte returnCandidate = lastBytecodeIs(0, RETURN_LOCAL);
    if (returnCandidate == INVALID) {
      return null;
    }

    if (bytecode.size() != (Bytecodes.getBytecodeLength(returnCandidate)
        + Bytecodes.getBytecodeLength(pushCandidate))) {
      return null;
    }

    byte idx = getIndex(1);
    // because we don't handle block methods, we don't need to worry about ctx > 0
    return new FieldReadNode(new LocalArgumentReadNode(arguments.get(universe.symSelf)), idx);
  }

  private ExpressionNode optimizeFieldSetter() {
    if (isBlockMethod()) {
      return null;
    }

    // example sequence: PUSH_ARG1 DUP POP_FIELD_1 RETURN_SELF
    final byte pushCandidate = lastBytecodeIs(3, PUSH_ARGUMENT);
    if (pushCandidate == INVALID) {
      return null;
    }

    if (getIndex(3) != 1) {
      // we only support access to the only true argument of a setter
      // (i.e. ignoring the receiver)
      // though, there could possibly be other arguments
      // TODO: lift the restriction
      return null;
    }

    final byte dupCandidate = lastBytecodeIs(2, DUP);
    if (dupCandidate == INVALID) {
      return null;
    }

    final byte popCandidate = lastBytecodeIs(1, POP_FIELD);
    if (popCandidate == INVALID) {
      return null;
    }

    final byte returnCandidate = lastBytecodeIs(0, RETURN_SELF);
    if (returnCandidate == INVALID) {
      return null;
    }

    byte fieldIdx = getIndex(1);
    Iterator<Argument> i = arguments.values().iterator();
    Argument self = i.next();
    Argument val = i.next();

    return FieldWriteNode.createForMethod(fieldIdx, self, val);
  }

  public boolean optimizeDupPopPopSequence() {
      // when we are inlining blocks, this already happened
      // and any new opportunities to apply these optimizations are consequently
      // at jump targets for blocks, and we can't remove those
      if (isCurrentlyInliningBlock) {
          return false;
      }
      
    final byte dupCandidate = lastBytecodeIs(1, DUP);
    final byte popCandidate = lastBytecodeIsOneOf(0, POP_LOCAL_FIELD_BYTECODES);

    if (popCandidate != INVALID && dupCandidate != INVALID) {
      if (POP_FIELD == popCandidate && optimizePushFieldIncDupPopField()) {
        return true;
      }

      removeLastBytecodeAt(1); // remove the DUP bytecode

      resetLastBytecodeBuffer();
      last4Bytecodes[3] = popCandidate;
      return true;
    }
    return false;
  }

  private byte getIndex(final int idxFromEnd) {
    int bcOffset = getOffsetOfLastBytecode(idxFromEnd);
    return bytecode.get(bcOffset + 1);
  }

  private byte[] getIndexAndContext(final int idxFromEnd) {
    byte actual = last4Bytecodes[last4Bytecodes.length - 1 - idxFromEnd];

    byte idx;
    byte ctx;

    switch (actual) {
      case PUSH_FIELD:
      case POP_FIELD: {
        int bcOffset = getOffsetOfLastBytecode(idxFromEnd);
        idx = bytecode.get(bcOffset + 1);
        ctx = bytecode.get(bcOffset + 2);
        break;
      }

      default:
        throw new NotYetImplementedException("Need to add support for more bytecodes");
    }

    return new byte[] {idx, ctx};
  }

  /**
   * This is going to try to optimize the following sequence, assuming that a pop would be
   * generated next.
   *
   * <pre>
   *   PUSH_FIELD
   *   INC
   *   DUP
   *   POP_FIELD
   * </pre>
   *
   * @return true, if it optimized it.
   */
  private boolean optimizePushFieldIncDupPopField() {
    if (lastBytecodeIsOneOf(3, PUSH_FIELD_BYTECODES) == INVALID) {
      return false;
    }
    if (lastBytecodeIsOneOf(2, INC_BYTECODES) == INVALID) {
      return false;
    }
    if (lastBytecodeIsOneOf(1, DUP_BYTECODES) == INVALID) {
      return false;
    }
    if (lastBytecodeIsOneOf(0, POP_FIELD_BYTECODES) == INVALID) {
      return false;
    }

    byte[] idxCtxPushField = getIndexAndContext(3);
    byte[] idxCtxPopField = getIndexAndContext(0);

    if (idxCtxPopField[0] == idxCtxPushField[0] && idxCtxPopField[1] == idxCtxPushField[1]) {
      // remove all four of the bytecodes, we just checked for
      removeLastBytecodes(4);

      resetLastBytecodeBuffer();
      BytecodeGenerator.emitINCFIELD(this, idxCtxPopField[0], idxCtxPopField[1]);
      return true;
    }
    return false;
  }

  public boolean inlineIfTrue() throws ParseError {
    // HACK: we do assume that the receiver on the stack is a boolean
    // HACK: similar to the {@see IfInlinedLiteralNode}
    // HACK: we don't support anything but booleans at the moment

    if (lastBytecodeIsOneOf(0, PUSH_BLOCK_BYTECODES) == INVALID) {
      return false;
    }

    assert Bytecodes.getBytecodeLength(PUSH_BLOCK) == 2;
    byte blockLiteralIdx = bytecode.get(bytecode.size() - 1);

    removeLastBytecodeAt(0); // remove the PUSH_BLOCK

    int jumpOffsetIdxToSkipTrueBranch = emitJumpOnFalseWithDummyOffset(this, false);

    // grab block's method, and inline it
    SMethod toBeInlined = (SMethod) literals.get(blockLiteralIdx);

    isCurrentlyInliningBlock = true;
    toBeInlined.getInvokable().inline(this, toBeInlined);
    isCurrentlyInliningBlock = false;

    patchJumpOffsetToPointToNextInstruction(jumpOffsetIdxToSkipTrueBranch);

    resetLastBytecodeBuffer();

    return true;
  }

  public boolean inlineIfFalse() {
    // HACK: we do assume that the receiver on the stack is a boolean
    // HACK: similar to the {@see IfInlinedLiteralNode}
    // HACK: we don't support anything but booleans at the moment

    if (lastBytecodeIsOneOf(0, PUSH_BLOCK_BYTECODES) == INVALID) {
      return false;
    }

    assert Bytecodes.getBytecodeLength(PUSH_BLOCK) == 2;
    byte blockLiteralIdx = bytecode.get(bytecode.size() - 1);

    removeLastBytecodes(1); // remove the PUSH_BLOCK

    int jumpOffsetIdxToSkipFalseBranch = emitJumpOnTrueWithDummyOffset(this, false);

    // grab block's method, and inline it
    SMethod toBeInlined = (SMethod) literals.get(blockLiteralIdx);
    isCurrentlyInliningBlock = true;
    toBeInlined.getInvokable().inline(this, toBeInlined);
    isCurrentlyInliningBlock = false;

    patchJumpOffsetToPointToNextInstruction(jumpOffsetIdxToSkipFalseBranch);

    resetLastBytecodeBuffer();

    return true;
  }

  public boolean inlineIfTrueIfFalse() {
    // HACK: we do assume that the receiver on the stack is a boolean
    // HACK: similar to the {@see IfInlinedLiteralNode}
    // HACK: we don't support anything but booleans at the moment

    if (lastBytecodeIsOneOf(0, PUSH_BLOCK_BYTECODES) == INVALID) {
      return false;
    }

    if (lastBytecodeIsOneOf(1, PUSH_BLOCK_BYTECODES) == INVALID) {
      return false;
    }

    assert Bytecodes.getBytecodeLength(PUSH_BLOCK) == 2;
    byte block1LiteralIdx = bytecode.get(bytecode.size() - 3);
    byte block2LiteralIdx = bytecode.get(bytecode.size() - 1);

    // grab block's method, and inline it
    SMethod toBeInlined1 = (SMethod) literals.get(block1LiteralIdx);
    SMethod toBeInlined2 = (SMethod) literals.get(block2LiteralIdx);

    removeLastBytecodes(2); // remove the PUSH_BLOCK bytecodes

    int jumpOffsetIdxToSkipTrueBranch = emitJumpOnFalseWithDummyOffset(this, true);

    isCurrentlyInliningBlock = true;
    toBeInlined1.getInvokable().inline(this, toBeInlined1);
    isCurrentlyInliningBlock = false;

    int jumpOffsetIdxToSkipFalseBranch = emitJumpWithDummyOffset(this);

    patchJumpOffsetToPointToNextInstruction(jumpOffsetIdxToSkipTrueBranch);
    resetLastBytecodeBuffer();

    isCurrentlyInliningBlock = true;
    toBeInlined2.getInvokable().inline(this, toBeInlined2);
    isCurrentlyInliningBlock = false;

    patchJumpOffsetToPointToNextInstruction(jumpOffsetIdxToSkipFalseBranch);
    resetLastBytecodeBuffer();

    return true;
  }

  public boolean inlineIfFalseIfTrue() {
    // HACK: we do assume that the receiver on the stack is a boolean
    // HACK: similar to the {@see IfInlinedLiteralNode}
    // HACK: we don't support anything but booleans at the moment

    if (lastBytecodeIsOneOf(0, PUSH_BLOCK_BYTECODES) == INVALID) {
      return false;
    }

    if (lastBytecodeIsOneOf(1, PUSH_BLOCK_BYTECODES) == INVALID) {
      return false;
    }

    assert Bytecodes.getBytecodeLength(PUSH_BLOCK) == 2;
    byte block1LiteralIdx = bytecode.get(bytecode.size() - 3);
    byte block2LiteralIdx = bytecode.get(bytecode.size() - 1);

    // grab block's method, and inline it
    SMethod toBeInlined1 = (SMethod) literals.get(block1LiteralIdx);
    SMethod toBeInlined2 = (SMethod) literals.get(block2LiteralIdx);

    removeLastBytecodes(2); // remove the PUSH_BLOCK bytecodes

    int jumpOffsetIdxToSkipFalseBranch = emitJumpOnTrueWithDummyOffset(this, true);

    isCurrentlyInliningBlock = true;
    toBeInlined1.getInvokable().inline(this, toBeInlined1);
    isCurrentlyInliningBlock = false;

    int jumpOffsetIdxToSkipTrueBranch = emitJumpWithDummyOffset(this);

    patchJumpOffsetToPointToNextInstruction(jumpOffsetIdxToSkipFalseBranch);
    resetLastBytecodeBuffer();

    isCurrentlyInliningBlock = true;
    toBeInlined2.getInvokable().inline(this, toBeInlined2);
    isCurrentlyInliningBlock = false;

    patchJumpOffsetToPointToNextInstruction(jumpOffsetIdxToSkipTrueBranch);
    resetLastBytecodeBuffer();

    return true;
  }

  private int computeStackDepth() {
    int depth = 0;
    int maxDepth = 0;
    int i = 0;

    while (i < bytecode.size()) {
      byte bc = bytecode.get(i);
      switch (bc) {
        case HALT:
          break;
        case DUP:
        case PUSH_LOCAL:
        case PUSH_ARGUMENT:
        case PUSH_FIELD:
        case PUSH_BLOCK:
        case PUSH_CONSTANT:
        case PUSH_GLOBAL:
          depth++;
          break;
        case POP:
        case POP_LOCAL:
        case POP_ARGUMENT:
        case POP_FIELD:
          depth--;
          break;
        case SEND:
        case SUPER_SEND: {
          // these are special: they need to look at the number of
          // arguments (extractable from the signature)
          SSymbol sig = (SSymbol) literals.get(bytecode.get(i + 1));

          depth -= sig.getNumberOfSignatureArguments();

          depth++; // return value
          break;
        }
        case INC:
        case DEC:
        case RETURN_LOCAL:
        case RETURN_SELF:
        case RETURN_NON_LOCAL:
        case INC_FIELD:
          break;
        case INC_FIELD_PUSH:
          depth++;
          break;
        case JUMP:
        case JUMP_ON_TRUE_TOP_NIL:
        case JUMP_ON_FALSE_TOP_NIL:
        case JUMP_ON_TRUE_POP:
        case JUMP_ON_FALSE_POP:
          break;
        default:
          throw new IllegalStateException("Illegal bytecode "
              + bytecode.get(i));
      }

      i += Bytecodes.getBytecodeLength(bc);

      if (depth > maxDepth) {
        maxDepth = depth;
      }
    }

    return maxDepth;
  }

  ArrayList<Byte> getBytecodes() {
    return bytecode;
  }
}
