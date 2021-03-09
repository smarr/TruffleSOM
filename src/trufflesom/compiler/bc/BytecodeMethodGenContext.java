package trufflesom.compiler.bc;

import static trufflesom.interpreter.bc.Bytecodes.DEC;
import static trufflesom.interpreter.bc.Bytecodes.DUP;
import static trufflesom.interpreter.bc.Bytecodes.HALT;
import static trufflesom.interpreter.bc.Bytecodes.INC;
import static trufflesom.interpreter.bc.Bytecodes.INC_FIELD;
import static trufflesom.interpreter.bc.Bytecodes.INC_FIELD_PUSH;
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
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.bc.Bytecodes;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.bc.BytecodeLoopNode;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SAbstractObject;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SSymbol;


public class BytecodeMethodGenContext extends MethodGenerationContext {

  private final List<Object>                  literals;
  private final LinkedHashMap<SSymbol, Local> outerVars;

  private final ArrayList<Byte> bytecode;
  private final byte[]          last4Bytecodes;

  private boolean finished;

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
    outerVars = new LinkedHashMap<>();
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

  public void addBytecodeArgument(final byte code) {
    bytecode.add(code);
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
    if (contextLevel == 0) {
      return getPositionIn(local, locals);
    }

    // we already got it, and it's in our outerVar list/map
    if (outerVars.containsValue(local)) {
      return (byte) (getPositionIn(local, outerVars) + locals.size());
    }

    int size = outerVars.size();
    outerVars.put(local.getName(), local);
    assert getPositionIn(local, outerVars) == size;

    return (byte) (size + locals.size());
  }

  private BytecodeLoopNode constructBytecodeBody(final SourceSection sourceSection) {
    byte[] bytecodes = new byte[bytecode.size()];
    int i = 0;
    for (byte bc : bytecode) {
      bytecodes[i] = bc;
      i += 1;
    }

    Object[] literalsArr = literals.toArray();
    FrameSlot[] localsAndOuters = new FrameSlot[locals.size() + outerVars.size()];

    i = 0;
    for (Local l : locals.values()) {
      localsAndOuters[i] = l.getSlot();
      i += 1;
    }

    for (Local l : outerVars.values()) {
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
    // TODO: recognize the sequences that can be isTrivial() expression nodes
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

  public boolean optimizeDupPopPopSequence() {
    final int size = bytecode.size();
    assert Bytecodes.getBytecodeLength(POP_LOCAL) == Bytecodes.getBytecodeLength(POP_FIELD);
    assert Bytecodes.getBytecodeLength(POP_LOCAL) == 3;
    assert Bytecodes.getBytecodeLength(DUP) == 1;

    if (size - 4 < 0) {
      return false;
    }

    final byte dupCandidate = last4Bytecodes[2];
    final byte popCandidate = last4Bytecodes[3];

    if ((popCandidate == POP_LOCAL || popCandidate == POP_FIELD) && dupCandidate == DUP) {
      if (popCandidate == POP_FIELD && optimizePushFieldIncDupPopField()) {
        return true;
      }

      // remove the DUP bytecode
      bytecode.remove(size - 4);

      last4Bytecodes[0] = -1;
      last4Bytecodes[1] = -1;
      last4Bytecodes[2] = -1;
      last4Bytecodes[3] = popCandidate;
      return true;
    }
    return false;
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
    final int size = bytecode.size();

    assert Bytecodes.getBytecodeLength(PUSH_FIELD) == 3;
    assert Bytecodes.getBytecodeLength(POP_FIELD) == 3;
    assert Bytecodes.getBytecodeLength(INC) == 1;
    assert Bytecodes.getBytecodeLength(DUP) == 1;

    if (size - (3 + 1 + 1 + 3) < 0) {
      return false;
    }

    final byte pushCandidate = last4Bytecodes[0];
    final byte incCandidate = last4Bytecodes[1];
    final byte dupCandidate = last4Bytecodes[2];
    final byte popCandidate = last4Bytecodes[3];

    final int pushBcIdx = size - 3 - 1 - 1 - 3;

    final byte pushFieldIdx = bytecode.get(pushBcIdx + 1);
    final byte pushCtxLevel = bytecode.get(pushBcIdx + 2);

    final int popBcIdx = size - 3;

    final byte popFieldIdx = bytecode.get(popBcIdx + 1);
    final byte popCtxLevel = bytecode.get(popBcIdx + 2);

    if (pushCandidate == PUSH_FIELD &&
        incCandidate == INC &&
        dupCandidate == DUP &&
        popCandidate == POP_FIELD &&
        pushFieldIdx == popFieldIdx && pushCtxLevel == popCtxLevel) {
      // remove the POP_FIELD bytecode
      bytecode.remove(bytecode.size() - 1);
      bytecode.remove(bytecode.size() - 1);
      bytecode.remove(bytecode.size() - 1);

      // remove the DUP bytecode
      bytecode.remove(bytecode.size() - 1);

      // remove the INC bytecode
      bytecode.remove(bytecode.size() - 1);

      // replace the PUSH_FIELD bytecode by INC_FIELD
      bytecode.set(pushBcIdx, INC_FIELD);

      last4Bytecodes[0] = -1;
      last4Bytecodes[1] = -1;
      last4Bytecodes[2] = -1;
      last4Bytecodes[3] = INC_FIELD;
      return true;
    }
    return false;
  }

  private int computeStackDepth() {
    int depth = 0;
    int maxDepth = 0;
    int i = 0;

    while (i < bytecode.size()) {
      int oldI = i;
      byte bc = bytecode.get(i);
      switch (bc) {
        case HALT:
          i++;
          break;
        case DUP:
          depth++;
          i++;
          break;
        case PUSH_LOCAL:
        case PUSH_ARGUMENT:
        case PUSH_FIELD:
          depth++;
          i += 3;
          break;
        case PUSH_BLOCK:
        case PUSH_CONSTANT:
        case PUSH_GLOBAL:
          depth++;
          i += 2;
          break;
        case POP:
          depth--;
          i++;
          break;
        case POP_LOCAL:
        case POP_ARGUMENT:
        case POP_FIELD:
          depth--;
          i += 3;
          break;
        case SEND:
        case SUPER_SEND: {
          // these are special: they need to look at the number of
          // arguments (extractable from the signature)
          SSymbol sig = (SSymbol) literals.get(bytecode.get(i + 1));

          depth -= sig.getNumberOfSignatureArguments();

          depth++; // return value
          i += 2;
          break;
        }
        case INC:
        case DEC:
        case RETURN_LOCAL:
        case RETURN_SELF:
          i++;
          break;
        case RETURN_NON_LOCAL:
          i += 2;
          break;
        case INC_FIELD:
          i += 3;
          break;
        case INC_FIELD_PUSH:
          i += 3;
          depth++;
          break;
        default:
          throw new IllegalStateException("Illegal bytecode "
              + bytecode.get(i));
      }

      assert oldI + Bytecodes.getBytecodeLength(bc) == i;

      if (depth > maxDepth) {
        maxDepth = depth;
      }
    }

    return maxDepth;
  }
}
