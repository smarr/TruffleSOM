package trufflesom.compiler.bc;

import static trufflesom.compiler.bc.BytecodeGenerator.emitJumpBackwardsWithOffset;
import static trufflesom.compiler.bc.BytecodeGenerator.emitJumpOnBoolWithDummyOffset;
import static trufflesom.compiler.bc.BytecodeGenerator.emitJumpWithDummyOffset;
import static trufflesom.compiler.bc.BytecodeGenerator.emitPOP;
import static trufflesom.compiler.bc.BytecodeGenerator.emitPUSHCONSTANT;
import static trufflesom.interpreter.bc.Bytecodes.DUP;
import static trufflesom.interpreter.bc.Bytecodes.INC;
import static trufflesom.interpreter.bc.Bytecodes.INC_FIELD;
import static trufflesom.interpreter.bc.Bytecodes.INC_FIELD_PUSH;
import static trufflesom.interpreter.bc.Bytecodes.INVALID;
import static trufflesom.interpreter.bc.Bytecodes.JUMP;
import static trufflesom.interpreter.bc.Bytecodes.JUMP2;
import static trufflesom.interpreter.bc.Bytecodes.JUMP_BYTECODES;
import static trufflesom.interpreter.bc.Bytecodes.POP;
import static trufflesom.interpreter.bc.Bytecodes.POP_ARGUMENT;
import static trufflesom.interpreter.bc.Bytecodes.POP_FIELD;
import static trufflesom.interpreter.bc.Bytecodes.POP_FIELD_0;
import static trufflesom.interpreter.bc.Bytecodes.POP_FIELD_1;
import static trufflesom.interpreter.bc.Bytecodes.POP_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.POP_LOCAL_0;
import static trufflesom.interpreter.bc.Bytecodes.POP_LOCAL_1;
import static trufflesom.interpreter.bc.Bytecodes.POP_LOCAL_2;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_0;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_1;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_ARG1;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_ARG2;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_ARGUMENT;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_BLOCK;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_BLOCK_NO_CTX;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_CONSTANT;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_CONSTANT_0;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_CONSTANT_1;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_CONSTANT_2;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_FIELD;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_FIELD_0;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_FIELD_1;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_GLOBAL;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_LOCAL_0;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_LOCAL_1;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_LOCAL_2;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_NIL;
import static trufflesom.interpreter.bc.Bytecodes.PUSH_SELF;
import static trufflesom.interpreter.bc.Bytecodes.RETURN_FIELD_0;
import static trufflesom.interpreter.bc.Bytecodes.RETURN_FIELD_1;
import static trufflesom.interpreter.bc.Bytecodes.RETURN_FIELD_2;
import static trufflesom.interpreter.bc.Bytecodes.RETURN_LOCAL;
import static trufflesom.interpreter.bc.Bytecodes.RETURN_SELF;
import static trufflesom.interpreter.bc.Bytecodes.getBytecodeLength;
import static trufflesom.vm.SymbolTable.symSelf;

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
import trufflesom.interpreter.nodes.bc.BytecodeLoopNode.BackJump;
import trufflesom.interpreter.nodes.literals.LiteralNode;
import trufflesom.vm.NotYetImplementedException;
import trufflesom.vm.Universe;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SAbstractObject;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SSymbol;


public class BytecodeMethodGenContext extends MethodGenerationContext {

  private final List<Object>                  literals;
  private final LinkedHashMap<SSymbol, Local> localAndOuterVars;

  private final ArrayList<BackJump> inlinedLoops;

  private final ArrayList<Byte> bytecode;
  private final byte[]          last4Bytecodes;

  private boolean finished;
  private boolean isCurrentlyInliningBlock = false;

  private int currentStackDepth;
  private int maxStackDepth;

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
    inlinedLoops = new ArrayList<>();
    localAndOuterVars = new LinkedHashMap<>();
    last4Bytecodes = new byte[4];
  }

  public Object getConstant(final int bytecodeIdx) {
    return literals.get(bytecode.get(bytecodeIdx + 1));
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
    markAccessingOuterScopes();
    return holderGenc.getFieldIndex(fieldName);
  }

  public byte findLiteralIndex(final Object lit) {
    return (byte) literals.indexOf(lit);
  }

  public int getStackDepth() {
    return maxStackDepth;
  }

  public void addBytecode(final byte code, final int stackEffect) {
    bytecode.add(code);

    currentStackDepth += stackEffect;
    maxStackDepth = Math.max(currentStackDepth, maxStackDepth);

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

  public void patchJumpOffsetToPointToNextInstruction(final int idxOfOffset,
      final ParserBc parser) throws ParseError {
    int instructionStart = idxOfOffset - 1;
    byte bytecodeBeforeOffset = bytecode.get(instructionStart);
    assert isOneOf(bytecodeBeforeOffset,
        JUMP_BYTECODES) : "Expected to patch a JUMP instruction, but got bc: "
            + bytecodeBeforeOffset;
    assert (JUMP2 - JUMP) == Bytecodes.NUM_1_BYTE_JUMP_BYTECODES // ~
        : "There's an unexpected number of JUMP bytecodes. Need to adapt the code below";
    int jumpOffset = bytecode.size() - instructionStart;

    checkJumpOffset(parser, jumpOffset);

    if (jumpOffset <= 0xff) {
      bytecode.set(idxOfOffset, (byte) jumpOffset);
      bytecode.set(idxOfOffset + 1, (byte) 0);
    } else {
      int offsetOfBytecode = idxOfOffset - 1;
      // we need two bytes for the jump offset
      bytecode.set(offsetOfBytecode,
          (byte) (bytecode.get(offsetOfBytecode) + Bytecodes.NUM_1_BYTE_JUMP_BYTECODES));

      byte byte1 = (byte) jumpOffset;
      byte byte2 = (byte) (jumpOffset >> 8);

      bytecode.set(idxOfOffset, byte1);
      bytecode.set(idxOfOffset + 1, byte2);
    }
  }

  private void checkJumpOffset(final ParserBc parser, final int jumpOffset) throws ParseError {
    if (jumpOffset < 0 || jumpOffset > 0xffff) {
      throw new ParseError(
          "The jumpOffset for the JUMP* bytecode is too large or small. jumpOffset="
              + jumpOffset,
          null, parser);
    }
  }

  public static int getJumpOffset(final byte byte1, final byte byte2) {
    int b1 = Byte.toUnsignedInt(byte1);
    int b2 = Byte.toUnsignedInt(byte2);

    return b1 + (b2 << 8);
  }

  public void emitBackwardsJumpOffsetToTarget(final int targetAddress, final ParserBc parser)
      throws ParseError {
    int addressOfJumpBc = offsetOfNextInstruction();

    // we are going to jump backward and want a positive value
    // thus we subtract target_address from address_of_jump
    int jumpOffset = addressOfJumpBc - targetAddress;

    checkJumpOffset(parser, jumpOffset);
    int backwardJumpIdx = offsetOfNextInstruction();

    byte byte1 = (byte) jumpOffset;
    byte byte2 = (byte) (jumpOffset >> 8);

    emitJumpBackwardsWithOffset(this, byte1, byte2);

    inlinedLoops.add(new BackJump(targetAddress, backwardJumpIdx));
  }

  public int offsetOfNextInstruction() {
    return bytecode.size();
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
    } else if (isOneOf(last4Bytecodes[3], POP_X_BYTECODES) && last4Bytecodes[2] != DUP) {
      // we just removed the DUP and didn't emit the POP using optimizeDupPopPopSequence()
      // so, to make blocks work, we need to reintroduce the DUP
      int idx = bytecode.size() - getBytecodeLength(last4Bytecodes[3]);
      assert idx >= 0;
      assert isOneOf(bytecode.get(idx), POP_X_BYTECODES);
      bytecode.add(idx, DUP);

      last4Bytecodes[0] = last4Bytecodes[1];
      last4Bytecodes[1] = last4Bytecodes[2];
      last4Bytecodes[2] = DUP;
    } else if (last4Bytecodes[3] == INC_FIELD) {
      // we optimized the sequence to an INC_FIELD, which doesn't modify the stack
      // but since we need the value to return it from the block, we need to push it.
      last4Bytecodes[3] = INC_FIELD_PUSH;

      int bcOffset = bytecode.size() - 3;
      assert Bytecodes.getBytecodeLength(INC_FIELD_PUSH) == 3;
      assert Bytecodes.getBytecodeLength(INC_FIELD) == 3;
      assert bytecode.get(bcOffset) == INC_FIELD;
      bytecode.set(bcOffset, INC_FIELD_PUSH);
    }
  }

  public byte addLiteralIfAbsent(final Object lit, final ParserBc parser)
      throws ParseError {
    int idx = literals.indexOf(lit);
    if (idx != -1) {
      return (byte) idx;
    }

    return addLiteral(lit, parser);
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
    byte[] bytecodes = getBytecodeArray();

    Object[] literalsArr = literals.toArray();
    FrameSlot[] localsAndOuters = new FrameSlot[localAndOuterVars.size()];

    int i = 0;
    for (Local l : localAndOuterVars.values()) {
      localsAndOuters[i] = l.getSlot();
      i += 1;
    }

    FrameSlot frameOnStackMarker =
        throwsNonLocalReturn ? getFrameOnStackMarker(sourceSection).getSlot() : null;

    BackJump[] loops = inlinedLoops.toArray(new BackJump[0]);

    return new BytecodeLoopNode(
        bytecodes, locals.size(), localsAndOuters, literalsArr, maxStackDepth,
        frameOnStackMarker, loops, universe);
  }

  public byte[] getBytecodeArray() {
    byte[] bytecodes = new byte[bytecode.size()];
    int i = 0;
    for (byte bc : bytecode) {
      bytecodes[i] = bc;
      i += 1;
    }
    return bytecodes;
  }

  private ExpressionNode constructTrivialBody() {
    byte returnCandidate = lastBytecodeIs(0, RETURN_LOCAL);
    if (returnCandidate != INVALID) {
      byte pushCandidate = lastBytecodeIsOneOf(1, PUSH_CONSTANT_BYTECODES);
      if (pushCandidate != INVALID) {
        return optimizeLiteralReturn(pushCandidate, returnCandidate);
      }

      pushCandidate = lastBytecodeIs(1, PUSH_GLOBAL);
      if (pushCandidate != INVALID) {
        return optimizeGlobalReturn(pushCandidate, returnCandidate);
      }

      return optimizeFieldGetter(false, returnCandidate);
    }

    // because we check for return_self here, we don't consider block methods
    returnCandidate = lastBytecodeIs(0, RETURN_SELF);
    if (returnCandidate != INVALID) {
      assert !isBlockMethod();
      return optimizeFieldSetter(returnCandidate);
    }

    returnCandidate = lastBytecodeIsOneOf(0, RETURN_FIELD_BYTECODES);
    if (returnCandidate != INVALID && bytecode.size() == 1) {
      return optimizeFieldGetter(true, returnCandidate);
    }

    return null;
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

  private boolean isOneOf(final byte bytecode, final byte[] candidates) {
    for (byte c : candidates) {
      if (c == bytecode) {
        return true;
      }
    }
    return false;
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

  private static final byte[] DUP_BYTECODES = new byte[] {DUP};
  private static final byte[] INC_BYTECODES = new byte[] {INC};

  private static final byte[] PUSH_BLOCK_BYTECODES =
      new byte[] {PUSH_BLOCK, PUSH_BLOCK_NO_CTX};

  private static final byte[] POP_X_BYTECODES = new byte[] {
      POP_LOCAL, POP_LOCAL_0, POP_LOCAL_1, POP_LOCAL_2,
      POP_ARGUMENT,
      POP_FIELD, POP_FIELD_0, POP_FIELD_1};

  private static final byte[] PUSH_FIELD_BYTECODES = new byte[] {
      PUSH_FIELD, PUSH_FIELD_0, PUSH_FIELD_1};

  private static final byte[] POP_FIELD_BYTECODES = new byte[] {
      POP_FIELD, POP_FIELD_0, POP_FIELD_1};

  private static final byte[] PUSH_NON_SELF_ARGUMENTS = new byte[] {
      PUSH_ARGUMENT, PUSH_ARG1, PUSH_ARG2};

  private static final byte[] RETURN_FIELD_BYTECODES = new byte[] {
      RETURN_FIELD_0, RETURN_FIELD_1, RETURN_FIELD_2};

  private static final byte[] PUSH_CONSTANT_BYTECODES = new byte[] {
      PUSH_CONSTANT, PUSH_CONSTANT_0, PUSH_CONSTANT_1, PUSH_CONSTANT_2,
      PUSH_0, PUSH_1, PUSH_NIL};

  private ExpressionNode optimizeLiteralReturn(final byte pushCandidate,
      final byte returnCandidate) {

    if (bytecode.size() != (Bytecodes.getBytecodeLength(returnCandidate)
        + Bytecodes.getBytecodeLength(pushCandidate))) {
      return null;
    }

    Object literal;
    if (pushCandidate == PUSH_0) {
      literal = 0L;
    } else if (pushCandidate == PUSH_1) {
      literal = 1L;
    } else if (pushCandidate == PUSH_NIL) {
      literal = Nil.nilObject;
    } else {
      byte constantIdx = getIndex(1);
      literal = literals.get(constantIdx);
    }
    return LiteralNode.create(literal);
  }

  private GlobalNode optimizeGlobalReturn(final byte pushCandidate,
      final byte returnCandidate) {
    if (bytecode.size() != (Bytecodes.getBytecodeLength(returnCandidate)
        + Bytecodes.getBytecodeLength(pushCandidate))) {
      return null;
    }

    byte constantIdx = getIndex(1);
    SSymbol literal = (SSymbol) literals.get(constantIdx);
    return GlobalNode.create(literal, universe, this);
  }

  private FieldReadNode optimizeFieldGetter(final boolean onlyReturnBytecode,
      final byte returnCandidate) {
    if (isBlockMethod()) {
      return null;
    }
    int idx = -1;

    if (onlyReturnBytecode) {
      idx = returnCandidate - RETURN_FIELD_0;
    } else {
      final byte pushCandidate = lastBytecodeIsOneOf(1, PUSH_FIELD_BYTECODES);
      if (pushCandidate == INVALID) {
        return null;
      }

      if (bytecode.size() != (Bytecodes.getBytecodeLength(returnCandidate)
          + Bytecodes.getBytecodeLength(pushCandidate))) {
        return null;
      }

      idx = getIndex(1);
    }

    if (idx == -1) {
      return null;
    }

    // because we don't handle block methods, we don't need to worry about ctx > 0
    return new FieldReadNode(new LocalArgumentReadNode(arguments.get(symSelf)), idx);
  }

  private ExpressionNode optimizeFieldSetter(final byte returnCandidate) {
    // example sequence: PUSH_ARG1 POP_FIELD_1 RETURN_SELF
    final byte pushCandidate = lastBytecodeIsOneOf(2, PUSH_NON_SELF_ARGUMENTS);
    if (pushCandidate == INVALID) {
      return null;
    }

    final byte popCandidate = lastBytecodeIsOneOf(1, POP_FIELD_BYTECODES);
    if (popCandidate == INVALID) {
      return null;
    }

    if (bytecode.size() != (Bytecodes.getBytecodeLength(returnCandidate)
        + Bytecodes.getBytecodeLength(pushCandidate)
        + Bytecodes.getBytecodeLength(popCandidate))) {
      return null;
    }

    byte argIdx = getIndex(2);
    byte fieldIdx = getIndex(1);
    Iterator<Argument> i = arguments.values().iterator();
    Argument self = i.next();
    Argument val = null;
    for (int j = 0; j < argIdx; j += 1) {
      val = i.next();
    }

    return FieldWriteNode.createForMethod(fieldIdx, self, val);
  }

  public boolean optimizeDupPopPopSequence() {
    // when we are inlining blocks, this already happened
    // and any new opportunities to apply these optimizations are consequently
    // at jump targets for blocks, and we can't remove those
    if (isCurrentlyInliningBlock) {
      return false;
    }

    if (lastBytecodeIs(0, INC_FIELD_PUSH) != INVALID) {
      return optimizeIncFieldPush();
    }

    final byte popCandidate = lastBytecodeIsOneOf(0, POP_X_BYTECODES);
    if (popCandidate == INVALID) {
      return false;
    }

    final byte dupCandidate = lastBytecodeIs(1, DUP);
    if (dupCandidate == INVALID) {
      return false;
    }

    removeLastBytecodeAt(1); // remove the DUP bytecode

    assert last4Bytecodes[3] == popCandidate;
    last4Bytecodes[2] = last4Bytecodes[1];
    last4Bytecodes[1] = last4Bytecodes[0];
    last4Bytecodes[0] = INVALID;

    return true;
  }

  private byte getIndex(final int idxFromEnd) {
    byte actual = last4Bytecodes[last4Bytecodes.length - 1 - idxFromEnd];

    switch (actual) {
      case PUSH_LOCAL_0:
      case PUSH_SELF:
      case PUSH_FIELD_0:
      case POP_LOCAL_0:
      case POP_FIELD_0:
      case RETURN_FIELD_0:
      case PUSH_CONSTANT_0: {
        return 0;
      }

      case PUSH_LOCAL_1:
      case PUSH_ARG1:
      case PUSH_FIELD_1:
      case POP_LOCAL_1:
      case POP_FIELD_1:
      case RETURN_FIELD_1:
      case PUSH_CONSTANT_1: {
        return 1;
      }

      case PUSH_LOCAL_2:
      case PUSH_ARG2:
      case POP_LOCAL_2:
      case RETURN_FIELD_2:
      case PUSH_CONSTANT_2: {
        return 2;
      }

      case PUSH_LOCAL:
      case PUSH_ARGUMENT:
      case PUSH_FIELD:
      case PUSH_BLOCK:
      case PUSH_CONSTANT:
      case PUSH_GLOBAL:
      case POP_LOCAL:
      case POP_ARGUMENT:
      case POP_FIELD:
      case INC_FIELD_PUSH: {
        int bcOffset = getOffsetOfLastBytecode(idxFromEnd);
        return bytecode.get(bcOffset + 1);
      }

      default:
        throw new NotYetImplementedException(
            "Need to add support for more bytecodes: " + Bytecodes.getBytecodeName(actual));
    }
  }

  private byte[] getIndexAndContext(final int idxFromEnd) {
    byte actual = last4Bytecodes[last4Bytecodes.length - 1 - idxFromEnd];

    byte idx;
    byte ctx;

    switch (actual) {
      case POP_FIELD_0:
      case PUSH_FIELD_0: {
        ctx = 0;
        idx = 0;
        break;
      }
      case POP_FIELD_1:
      case PUSH_FIELD_1: {
        ctx = 0;
        idx = 1;
        break;
      }

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

  private boolean optimizeIncFieldPush() {
    assert Bytecodes.getBytecodeLength(INC_FIELD_PUSH) == 3;

    int bcIdx = bytecode.size() - 3;
    assert bytecode.get(bcIdx) == INC_FIELD_PUSH;

    bytecode.set(bcIdx, INC_FIELD);
    last4Bytecodes[3] = INC_FIELD;

    return true;
  }

  /**
   * Try using a INC_FIELD bytecode instead of the following sequence.
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
  public boolean optimizeIncField(final byte fieldIdx, final byte ctx) {
    if (isCurrentlyInliningBlock) {
      return false;
    }

    if (lastBytecodeIs(0, DUP) == INVALID) {
      return false;
    }
    if (lastBytecodeIs(1, INC) == INVALID) {
      return false;
    }

    if (lastBytecodeIsOneOf(2, PUSH_FIELD_BYTECODES) == INVALID) {
      return false;
    }

    byte[] idxCtxPushField = getIndexAndContext(2);

    if (fieldIdx == idxCtxPushField[0] && ctx == idxCtxPushField[1]) {
      // remove all four of the bytecodes, we just checked for
      removeLastBytecodes(3);

      resetLastBytecodeBuffer();
      BytecodeGenerator.emitINCFIELDPUSH(this, fieldIdx, ctx);
      return true;
    }
    return false;
  }

  /**
   * This is going to try to optimize PUSH_FIELD_n, RETURN_LOCAL sequences.
   * The RETURN_LOCAL hasn't been written yet, so, we only need to check the PUSH_FIELD_n
   * bytecode.
   *
   * @return true, if optimized
   */
  public boolean optimizeReturnField() {
    // when we are inlining blocks, this already happened
    // and any new opportunities to apply these optimizations
    // may interfere with jump targets
    if (isCurrentlyInliningBlock) {
      return false;
    }

    byte pushFieldCandidate = lastBytecodeIsOneOf(0, PUSH_FIELD_BYTECODES);
    if (pushFieldCandidate == INVALID) {
      return false;
    }

    byte[] idxCtxPushField = getIndexAndContext(0);
    if (idxCtxPushField[1] != 0) {
      return false;
    }

    if (idxCtxPushField[0] > 2) {
      return false;
    }

    removeLastBytecodes(1); // remove the PUSH_FIELD_n bytecode
    resetLastBytecodeBuffer();

    BytecodeGenerator.emitRETURNFIELD(this, idxCtxPushField[0]);
    return true;
  }

  public boolean inlineIfTrueOrIfFalse(final ParserBc parser, final boolean ifTrue)
      throws ParseError {
    // HACK: we do assume that the receiver on the stack is a boolean
    // HACK: similar to the {@see IfInlinedLiteralNode}
    // HACK: we don't support anything but booleans at the moment

    if (lastBytecodeIsOneOf(0, PUSH_BLOCK_BYTECODES) == INVALID) {
      return false;
    }

    assert Bytecodes.getBytecodeLength(PUSH_BLOCK) == 2;
    byte blockLiteralIdx = bytecode.get(bytecode.size() - 1);

    removeLastBytecodeAt(0); // remove the PUSH_BLOCK

    int jumpOffsetIdxToSkipTrueBranch = emitJumpOnBoolWithDummyOffset(this, ifTrue, false);

    // grab block's method, and inline it
    SMethod toBeInlined = (SMethod) literals.get(blockLiteralIdx);

    isCurrentlyInliningBlock = true;
    toBeInlined.getInvokable().inline(this, toBeInlined);
    isCurrentlyInliningBlock = false;

    patchJumpOffsetToPointToNextInstruction(jumpOffsetIdxToSkipTrueBranch, parser);

    resetLastBytecodeBuffer();

    return true;
  }

  public boolean inlineIfTrueIfFalse(final ParserBc parser, final boolean isIfTrueIfFalse)
      throws ParseError {
    // HACK: we do assume that the receiver on the stack is a boolean
    // HACK: similar to the {@see IfInlinedLiteralNode}
    // HACK: we don't support anything but booleans at the moment

    if (!hasTwoLiteralBlockArguments()) {
      return false;
    }

    assert Bytecodes.getBytecodeLength(PUSH_BLOCK) == 2;
    assert Bytecodes.getBytecodeLength(PUSH_BLOCK_NO_CTX) == 2;
    byte block1LiteralIdx = bytecode.get(bytecode.size() - 3);
    byte block2LiteralIdx = bytecode.get(bytecode.size() - 1);

    // grab block's method, and inline it
    SMethod toBeInlined1 = (SMethod) literals.get(block1LiteralIdx);
    SMethod toBeInlined2 = (SMethod) literals.get(block2LiteralIdx);

    removeLastBytecodes(2); // remove the PUSH_BLOCK bytecodes

    int jumpOffsetIdxToSkipTrueBranch =
        emitJumpOnBoolWithDummyOffset(this, isIfTrueIfFalse, true);

    isCurrentlyInliningBlock = true;
    toBeInlined1.getInvokable().inline(this, toBeInlined1);

    int jumpOffsetIdxToSkipFalseBranch = emitJumpWithDummyOffset(this);

    patchJumpOffsetToPointToNextInstruction(jumpOffsetIdxToSkipTrueBranch, parser);
    resetLastBytecodeBuffer();

    toBeInlined2.getInvokable().inline(this, toBeInlined2);
    isCurrentlyInliningBlock = false;

    patchJumpOffsetToPointToNextInstruction(jumpOffsetIdxToSkipFalseBranch, parser);
    resetLastBytecodeBuffer();

    return true;
  }

  public boolean inlineWhileTrueOrFalse(final ParserBc parser, final boolean isWhileTrue)
      throws ParseError {
    if (!hasTwoLiteralBlockArguments()) {
      return false;
    }

    assert Bytecodes.getBytecodeLength(PUSH_BLOCK) == 2;
    assert Bytecodes.getBytecodeLength(PUSH_BLOCK_NO_CTX) == 2;
    byte block1LiteralIdx = bytecode.get(bytecode.size() - 3);
    byte block2LiteralIdx = bytecode.get(bytecode.size() - 1);

    // grab block's method, and inline it
    SMethod condMethod = (SMethod) literals.get(block1LiteralIdx);
    SMethod bodyMethod = (SMethod) literals.get(block2LiteralIdx);

    removeLastBytecodes(2); // remove the PUSH_BLOCK bytecodes

    int loopBeginIdx = offsetOfNextInstruction();

    isCurrentlyInliningBlock = true;
    condMethod.getInvokable().inline(this, condMethod);

    int jumpOffsetIdxToSkipLoopBody = emitJumpOnBoolWithDummyOffset(this, isWhileTrue, true);

    bodyMethod.getInvokable().inline(this, bodyMethod);

    completeJumpsAndEmitReturningNil(parser, loopBeginIdx, jumpOffsetIdxToSkipLoopBody);

    isCurrentlyInliningBlock = false;
    return true;
  }

  private boolean hasTwoLiteralBlockArguments() {
    if (lastBytecodeIsOneOf(1, PUSH_BLOCK_BYTECODES) == INVALID) {
      return false;
    }

    if (lastBytecodeIsOneOf(0, PUSH_BLOCK_BYTECODES) == INVALID) {
      return false;
    }
    return true;
  }

  private void completeJumpsAndEmitReturningNil(final ParserBc parser, final int loopBeginIdx,
      final int jumpOffsetIdxToSkipLoopBody) throws ParseError {
    resetLastBytecodeBuffer();
    emitPOP(this);

    emitBackwardsJumpOffsetToTarget(loopBeginIdx, parser);

    patchJumpOffsetToPointToNextInstruction(jumpOffsetIdxToSkipLoopBody, parser);

    emitPUSHCONSTANT(this, Nil.nilObject, parser);

    resetLastBytecodeBuffer();
  }

  public ArrayList<Byte> getBytecodes() {
    return bytecode;
  }

  public Universe getUniverse() {
    return universe;
  }
}
