package trufflesom.compiler.bc;

import static trufflesom.interpreter.bc.Bytecodes.DUP;
import static trufflesom.interpreter.bc.Bytecodes.HALT;
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
import static trufflesom.interpreter.bc.Bytecodes.SEND;
import static trufflesom.interpreter.bc.Bytecodes.SUPER_SEND;

import java.util.ArrayList;
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
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.bc.BytecodeLoopNode;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SAbstractObject;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SSymbol;


public class BytecodeMethodGenContext extends MethodGenerationContext {

  private final List<Object> literals;

  private final ArrayList<Byte> bytecode;
  private boolean               finished;

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

  public void removeLastBytecode() {
    bytecode.remove(bytecode.size() - 1);
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
      final SAbstractObject newVal) {
    assert literals.get(index) == oldVal;
    literals.set(index, newVal);
  }

  public byte getLocalIndex(final Local local, final int contextLevel) {
    int ctxLevel = contextLevel;
    while (ctxLevel > 0) {
      return ((BytecodeMethodGenContext) outerGenc).getLocalIndex(local, contextLevel - 1);
    }

    byte i = 0;
    for (Local l : locals.values()) {
      if (l == local) {
        return i;
      }
      i += 1;
    }
    return -1;
  }

  @Override
  protected SInvokable assembleMethod(final ExpressionNode unused,
      final SourceSection sourceSection, final SourceSection fullSourceSection) {
    byte[] bytecodes = new byte[bytecode.size()];
    int i = 0;
    for (byte bc : bytecode) {
      bytecodes[i] = bc;
      i += 1;
    }

    Object[] literalsArr = literals.toArray();
    FrameSlot[] localsArr = new FrameSlot[locals.size()];

    i = 0;
    for (Local l : locals.values()) {
      localsArr[i] = l.getSlot();
      i += 1;
    }

    ExpressionNode body = new BytecodeLoopNode(
        bytecodes, localsArr, literalsArr, computeStackDepth(), universe);
    body.initialize(sourceSection);

    return super.assembleMethod(body, sourceSection, fullSourceSection);
  }

  private int computeStackDepth() {
    int depth = 0;
    int maxDepth = 0;
    int i = 0;

    while (i < bytecode.size()) {
      switch (bytecode.get(i)) {
        case HALT:
          i++;
          break;
        case DUP:
          depth++;
          i++;
          break;
        case PUSH_LOCAL:
        case PUSH_ARGUMENT:
          depth++;
          i += 3;
          break;
        case PUSH_FIELD:
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
          depth--;
          i += 3;
          break;
        case POP_FIELD:
          depth--;
          i += 2;
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
        case RETURN_LOCAL:
        case RETURN_NON_LOCAL:
          i++;
          break;
        default:
          throw new IllegalStateException("Illegal bytecode "
              + bytecode.get(i));
      }

      if (depth > maxDepth) {
        maxDepth = depth;
      }
    }

    return maxDepth;
  }
}
