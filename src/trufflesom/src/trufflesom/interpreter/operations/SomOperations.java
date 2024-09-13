package trufflesom.interpreter.operations;

import java.util.HashMap;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.bytecode.BytecodeLocal;
import com.oracle.truffle.api.bytecode.BytecodeNode;
import com.oracle.truffle.api.bytecode.BytecodeRootNode;
import com.oracle.truffle.api.bytecode.ConstantOperand;
import com.oracle.truffle.api.bytecode.EpilogExceptional;
import com.oracle.truffle.api.bytecode.EpilogReturn;
import com.oracle.truffle.api.bytecode.GenerateBytecode;
import com.oracle.truffle.api.bytecode.LocalSetter;
import com.oracle.truffle.api.bytecode.Operation;
import com.oracle.truffle.api.bytecode.OperationProxy;
import com.oracle.truffle.api.bytecode.Variadic;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.Source;

import trufflesom.compiler.MethodGenerationContext;
import trufflesom.compiler.Variable.Internal;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.FrameOnStackMarker;
import trufflesom.interpreter.Invokable;
import trufflesom.interpreter.ReturnException;
import trufflesom.interpreter.SomLanguage;
import trufflesom.interpreter.Types;
import trufflesom.interpreter.nodes.ContextualNode;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.specialized.AndBoolMessageNode;
import trufflesom.interpreter.nodes.specialized.NotMessageNode;
import trufflesom.interpreter.nodes.specialized.OrBoolMessageNode;
import trufflesom.interpreter.nodes.specialized.whileloops.WhileFalsePrimitiveNode;
import trufflesom.interpreter.nodes.specialized.whileloops.WhileTruePrimitiveNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.IncrementLongFieldNode;
import trufflesom.interpreter.operations.copied.AdditionOp;
import trufflesom.interpreter.operations.copied.ArrayDoIndexesOp;
import trufflesom.interpreter.operations.copied.ArrayDoOp;
import trufflesom.interpreter.operations.copied.ArrayPutAllOp;
import trufflesom.interpreter.operations.copied.CharAtOp;
import trufflesom.interpreter.operations.copied.EqualsOp;
import trufflesom.interpreter.operations.copied.IfMessageOp;
import trufflesom.interpreter.operations.copied.NonLocalArgumentReadOp;
import trufflesom.interpreter.operations.copied.SubtractionOp;
import trufflesom.interpreter.supernodes.IntIncrementNode;
import trufflesom.primitives.arithmetic.BitXorPrim;
import trufflesom.primitives.arithmetic.DividePrim;
import trufflesom.primitives.arithmetic.DoubleDivPrim;
import trufflesom.primitives.arithmetic.GreaterThanOrEqualPrim;
import trufflesom.primitives.arithmetic.GreaterThanPrim;
import trufflesom.primitives.arithmetic.LessThanOrEqualPrim;
import trufflesom.primitives.arithmetic.LessThanPrim;
import trufflesom.primitives.arithmetic.LogicAndPrim;
import trufflesom.primitives.arithmetic.ModuloPrim;
import trufflesom.primitives.arithmetic.MultiplicationPrim;
import trufflesom.primitives.arithmetic.RemainderPrim;
import trufflesom.primitives.basics.EqualsEqualsPrim;
import trufflesom.primitives.basics.IntegerPrims.AbsPrim;
import trufflesom.primitives.basics.IntegerPrims.As32BitSignedValue;
import trufflesom.primitives.basics.IntegerPrims.As32BitUnsignedValue;
import trufflesom.primitives.basics.IntegerPrims.AsDoubleValue;
import trufflesom.primitives.basics.IntegerPrims.LeftShiftPrim;
import trufflesom.primitives.basics.IntegerPrims.MaxIntPrim;
import trufflesom.primitives.basics.IntegerPrims.MinIntPrim;
import trufflesom.primitives.basics.IntegerPrims.NegatedValue;
import trufflesom.primitives.basics.IntegerPrims.ToPrim;
import trufflesom.primitives.basics.IntegerPrims.UnsignedRightShiftPrim;
import trufflesom.primitives.basics.NewObjectPrim;
import trufflesom.primitives.basics.StringPrims.ConcatPrim;
import trufflesom.primitives.basics.UnequalUnequalPrim;
import trufflesom.primitives.basics.UnequalsPrim;
import trufflesom.primitives.reflection.ObjectPrims.InstVarAtPrim;
import trufflesom.primitives.reflection.ObjectPrims.InstVarAtPutPrim;
import trufflesom.primitives.reflection.ObjectPrims.IsNilNode;
import trufflesom.primitives.reflection.ObjectPrims.NotNilNode;
import trufflesom.vm.Classes;
import trufflesom.vm.Globals;
import trufflesom.vm.Globals.Association;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SAbstractObject;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


@GenerateBytecode(languageClass = SomLanguage.class, boxingEliminationTypes = {long.class, double.class})
@TypeSystemReference(Types.class)
@OperationProxy(SubtractionOp.class)
@OperationProxy(AdditionOp.class)
@OperationProxy(MultiplicationPrim.class)
@OperationProxy(EqualsOp.class)
@OperationProxy(EqualsEqualsPrim.class)
@OperationProxy(UnequalsPrim.class)
@OperationProxy(NotMessageNode.class)
@OperationProxy(NotNilNode.class)
@OperationProxy(IsNilNode.class)
@OperationProxy(NonLocalArgumentReadOp.class)
@OperationProxy(ArrayDoOp.class)
@OperationProxy(ArrayDoIndexesOp.class)
@OperationProxy(ArrayPutAllOp.class)
@OperationProxy(IntIncrementNode.class)
@OperationProxy(LessThanPrim.class)
@OperationProxy(LessThanOrEqualPrim.class)
@OperationProxy(GreaterThanPrim.class)
@OperationProxy(GreaterThanOrEqualPrim.class)
@OperationProxy(DividePrim.class)
@OperationProxy(AndBoolMessageNode.class)
@OperationProxy(OrBoolMessageNode.class)
@OperationProxy(CharAtOp.class)
@OperationProxy(ConcatPrim.class)
@OperationProxy(WhileTruePrimitiveNode.class)
@OperationProxy(WhileFalsePrimitiveNode.class)
@OperationProxy(MaxIntPrim.class)
@OperationProxy(MinIntPrim.class)
@OperationProxy(DoubleDivPrim.class)
@OperationProxy(NewObjectPrim.class)
@OperationProxy(ModuloPrim.class)
@OperationProxy(ToPrim.class)
@OperationProxy(UnequalUnequalPrim.class)
@OperationProxy(AbsPrim.class)
@OperationProxy(NegatedValue.class)
@OperationProxy(LeftShiftPrim.class)
@OperationProxy(RemainderPrim.class)
@OperationProxy(LogicAndPrim.class)
@OperationProxy(BitXorPrim.class)
@OperationProxy(As32BitSignedValue.class)
@OperationProxy(As32BitUnsignedValue.class)
@OperationProxy(AsDoubleValue.class)
@OperationProxy(UnsignedRightShiftPrim.class)
@OperationProxy(InstVarAtPrim.class)
@OperationProxy(InstVarAtPutPrim.class)
@OperationProxy(IfMessageOp.class)
public abstract class SomOperations extends Invokable implements BytecodeRootNode {

  @CompilationFinal private BytecodeLocal frameOnStackMarker;

  @CompilationFinal private int frameOnStackMarkerIdx;

  protected SomOperations(
      final SomLanguage language,
      final FrameDescriptor.Builder frameDescriptorBuilder) {
    super(null, null, 0, makeDescriptorWithNil(frameDescriptorBuilder));
    frameOnStackMarkerIdx = -1;
  }

  public void initialize(final String n, final Source s, final long sc) {
    this.name = n;
    this.source = s;
    this.sourceCoord = sc;
  }

  public void setFrameOnStackMarker(final BytecodeLocal frameOnStackMarker) {
    if (frameOnStackMarker != null) {
      this.frameOnStackMarker = frameOnStackMarker;
      this.frameOnStackMarkerIdx = frameOnStackMarker.getLocalOffset() + 1; // hack. DSL bug???
    }
  }

  private static FrameDescriptor makeDescriptorWithNil(final FrameDescriptor.Builder builder) {
    builder.defaultValue(Nil.nilObject);
    return builder.build();
  }

  @Override
  public HashMap<Local, BytecodeLocal> createLocals(final SomOperationsGen.Builder opBuilder) {
    return null;
  }

  @Override
  public ExpressionNode inline(final MethodGenerationContext targetMgenc,
      final SMethod toBeInlined) {
    return null;
  }

  @Override
  public Invokable convertMethod(final SomOperationsGen.Builder opBuilder,
      final LexicalScopeForOp scope) {
    return this;
  }

  @Override
  public boolean isTrivial() {
    return false;
  }

  public static final class LexicalScopeForOp {
    public final HashMap<Local, BytecodeLocal> opLocals;
    public final LexicalScopeForOp             outer;

    public LexicalScopeForOp(final HashMap<Local, BytecodeLocal> opLocals,
        final LexicalScopeForOp outer) {
      this.opLocals = opLocals;
      this.outer = outer;
    }

    public BytecodeLocal getOnStackMarker() {
      for (var e : opLocals.entrySet()) {
        if (e.getKey() instanceof Internal i) {
          if (i.getName().equals(SymbolTable.strFrameOnStack)) {
            return e.getValue();
          }
        }
      }
      return null;
    }
  }

  @Operation
  @ConstantOperand(type = int.class)
  public static final class DetermineContextOp {

    @Specialization
    public static MaterializedFrame determineContext(final VirtualFrame frame,
        final int contextLevel) {
      return ContextualNode.determineContext(frame, contextLevel);
    }
  }

  @Operation
  @ImportStatic(Types.class)
  @ConstantOperand(type = SSymbol.class)
  public static final class MessageSendOp {
    public static Object getRcvr(final Object[] arguments) {
      return arguments[0];
    }

    @Specialization
    public static Object doCached(final VirtualFrame frame,
        @SuppressWarnings("unused") final SSymbol selector,
        @Variadic final Object[] arguments,
        @Cached("create(selector)") final AbstractDispatchNode dispatch) {
      return dispatch.executeDispatch(frame, arguments);
    }

    // TODO: uncached case
  }

  @Operation
  @ConstantOperand(type = CallTarget.class)
  public static final class SuperSendOp {
    @Specialization
    public static Object doSuper(final CallTarget callTarget,
                                 @Variadic final Object[] arguments,
                                 @Cached("create(callTarget)") DirectCallNode callNode) {
      return callNode.call(arguments);
    }
  }

  @Operation
  @ConstantOperand(type = SSymbol.class)
  @ImportStatic(Globals.class)
  public static final class GlobalReadOp {
    @Specialization(guards = "assoc != null")
    public static Object doCached(@SuppressWarnings("unused") final SSymbol globalName,
        @Cached(value = "getGlobalsAssociation(globalName)",
            neverDefault = false) final Association assoc) {
      return assoc.getValue();
    }

    @Specialization(guards = {"assoc == null", "freshLookupResult == null"})
    public static Object doCached(final VirtualFrame frame, final SSymbol globalName,
        @SuppressWarnings("unused") @Bind("getGlobalsAssociation(globalName)") final Association freshLookupResult,
        @SuppressWarnings("unused") @Cached(value = "getGlobalsAssociation(globalName)",
            neverDefault = false) final Association assoc) {

      Object self = frame.getArguments()[0];
      return sendUnknownGlobalToOuter(globalName, self);
    }

    @TruffleBoundary
    private static Object sendUnknownGlobalToOuter(final SSymbol globalName,
        final Object selfObj) {
      Object self = selfObj;
      // find outer self
      while (self instanceof SBlock) {
        self = ((SBlock) self).getOuterSelf();
      }

      // if it is not defined, we will send a error message to the current
      // receiver object

      return SAbstractObject.sendUnknownGlobal(self, globalName);
    }
  }

  @Operation
  @ConstantOperand(type = Association.class)
  public static final class GlobalCachedReadOp {
    @NeverDefault
    public static Assumption get(final Association assoc) {
      return assoc.getAssumption();
    }

    @Specialization // TODO: debug DSL bug: (assumptions = "cachedAssumption", limit = "1")
    public static Object doCached(final Association assoc,
        @Cached("get(assoc)") final Assumption cachedAssumption) {
      return assoc.getValue();
    }
  }

  @NeverDefault
  protected static SClass getBlockClass(final SMethod blockMethod) {
    return Classes.getBlockClass(blockMethod.getNumberOfArguments());
  }

  @Operation
  @ImportStatic(SomOperations.class)
  @ConstantOperand(type = SMethod.class)
  public static final class PushBlockWithoutContextOp {
    @Specialization
    public static SBlock instantiate(final SMethod blockMethod,
        @Cached("getBlockClass(blockMethod)") final SClass blockClass) {
      return new SBlock(blockMethod, blockClass, null);
    }
  }

  @Operation
  @ImportStatic(SomOperations.class)
  @ConstantOperand(type = SMethod.class)
  public static final class PushBlockWithContextOp {
    @Specialization
    public static SBlock instantiate(final VirtualFrame frame, final SMethod blockMethod,
        @Cached("getBlockClass(blockMethod)") final SClass blockClass) {
      return new SBlock(blockMethod, blockClass, frame.materialize());
    }
  }

  @Operation
  public static final class LoopBoundLessOrEqual {
    @Specialization
    public static boolean isLessOrEqual(final long a, final long b) {
      return a <= b;
    }

    @Specialization
    public static boolean isLessOrEqual(final long a, final double b) {
      return a <= b;
    }

    @Specialization
    public static boolean isLessOrEqual(final double a, final double b) {
      return a <= b;
    }

    @Specialization
    public static boolean isLessOrEqual(final double a, final long b) {
      return a <= b;
    }
  }

  @Operation
  @ConstantOperand(type = LocalSetter.class)
  public static final class UnsafeLoopIncrement {
    @Specialization
    public static void increment(final VirtualFrame frame,
        final LocalSetter setter,
        final long currentValue,
        @Bind BytecodeNode bytecodeNode,
        @Bind("$bytecodeIndex") int bci) {
      setter.setLong(bytecodeNode, bci, frame, currentValue + 1);
    }

    @Specialization
    public static void increment(final VirtualFrame frame,
        final LocalSetter setter,
        final double currentValue,
        @Bind BytecodeNode bytecodeNode,
        @Bind("$bytecodeIndex") int bci) {
      setter.setDouble(bytecodeNode, bci, frame, currentValue + 1.0);
    }
  }

  @Operation
  @ConstantOperand(type = LocalSetter.class)
  public static final class UnsafeLoopDecrement {
    @Specialization
    public static void increment(final VirtualFrame frame,
        final LocalSetter setter,
        final long currentValue,
        @Bind BytecodeNode bytecodeNode,
        @Bind("$bytecodeIndex") int bci) {
      setter.setLong(bytecodeNode, bci, frame, currentValue - 1);
    }

    @Specialization
    public static void increment(final VirtualFrame frame,
        final LocalSetter setter,
        final double currentValue,
        @Bind BytecodeNode bytecodeNode,
        @Bind("$bytecodeIndex") int bci) {
      setter.setDouble(bytecodeNode, bci, frame, currentValue - 1.0);
    }
  }

  @Operation
  @ConstantOperand(type = int.class)
  @ImportStatic(FieldAccessorNode.class)
  public static final class ReadField {
    @Specialization(rewriteOn = UnexpectedResultException.class)
    public static long readLong(
        @SuppressWarnings("unused") final int fieldIdx,
        @SuppressWarnings("unused") final SObject self,
        @Cached("createRead(fieldIdx)") final AbstractReadFieldNode read)
        throws UnexpectedResultException {
      return read.readLong(self);
    }

    @Specialization(rewriteOn = UnexpectedResultException.class)
    public static double readDouble(
        @SuppressWarnings("unused") final int fieldIdx,
        @SuppressWarnings("unused") final SObject self,
        @Cached("createRead(fieldIdx)") final AbstractReadFieldNode read)
        throws UnexpectedResultException {
      return read.readDouble(self);
    }

    @Specialization
    public static Object readObject(
        @SuppressWarnings("unused") final int fieldIdx,
        @SuppressWarnings("unused") final SObject self,
        @Cached("createRead(fieldIdx)") final AbstractReadFieldNode read) {
      return read.read(self);
    }
  }

  @Operation
  @ImportStatic(FieldAccessorNode.class)
  @ConstantOperand(type = int.class)
  public static final class WriteField {
    @Specialization
    public static long writeLong(
        @SuppressWarnings("unused") final int fieldIdx,
        final SObject self, final long value,
        @Cached("createWrite(fieldIdx)") final AbstractWriteFieldNode write) {
      return write.write(self, value);
    }

    @Specialization
    public static double writeDouble(
        @SuppressWarnings("unused") final int fieldIdx,
        final SObject self, final double value,
        @Cached("createWrite(fieldIdx)") final AbstractWriteFieldNode write) {
      return write.write(self, value);
    }

    @Specialization
    public static Object writeObject(
        @SuppressWarnings("unused") final int fieldIdx,
        final SObject self, final Object value,
        @Cached("createWrite(fieldIdx)") final AbstractWriteFieldNode write) {
      return write.write(self, value);
    }
  }

  @Operation
  @ConstantOperand(type = int.class)
  @ImportStatic(FieldAccessorNode.class)
  public static final class WriteFieldAndReturnSelf {
    @Specialization
    public static SObject writeLong(
        @SuppressWarnings("unused") final int fieldIdx,
        final SObject self, final long value,
        @Cached("createWrite(fieldIdx)") final AbstractWriteFieldNode write) {
      write.write(self, value);
      return self;
    }

    @Specialization
    public static SObject writeDouble(
        @SuppressWarnings("unused") final int fieldIdx,
        final SObject self, final double value,
        @Cached("createWrite(fieldIdx)") final AbstractWriteFieldNode write) {
      write.write(self, value);
      return self;
    }

    @Specialization
    public static SObject writeObject(
        @SuppressWarnings("unused") final int fieldIdx,
        final SObject self, final Object value,
        @Cached("createWrite(fieldIdx)") final AbstractWriteFieldNode write) {
      write.write(self, value);
      return self;
    }
  }

  @Operation
  @ImportStatic(FieldAccessorNode.class)
  @ConstantOperand(type = int.class)
  public static final class IncField {
    @Specialization
    public static long incLong(
        @SuppressWarnings("unused") final int fieldIdx,
        final SObject self,
        @Cached("createIncrement(fieldIdx, self)") final IncrementLongFieldNode inc) {
      return inc.increment(self);
    }
  }

  @Operation
  @ConstantOperand(type = int.class)
  public static final class WriteArgument {
    @Specialization
    public static Object writeArg(final VirtualFrame frame, final int argIdx,
        final Object value) {
      frame.getArguments()[argIdx] = value;
      return value;
    }
  }

  @Operation
  @ConstantOperand(type = int.class)
  public static final class ReturnNonLocal {
    @Specialization
    public static Object doReturn(final VirtualFrame frame,
        int contextLevel,
        final Object result,
        final FrameOnStackMarker marker) {
      if (marker.isOnStack()) {
        throw new ReturnException(result, marker);
      } else {
        // TODO: add branch profile
        MaterializedFrame ctx = ContextualNode.determineContext(frame, contextLevel);
        SBlock block = (SBlock) frame.getArguments()[0];
        Object self = ctx.getArguments()[0];
        return SAbstractObject.sendEscapedBlock(self, block);
      }
    }
  }

  @Operation
  public static final class NewOnStackMarker {
    @Specialization
    public static FrameOnStackMarker createMarker() {
      return new FrameOnStackMarker();
    }
  }

  @EpilogExceptional
  public static final class ExecuteEpilogExceptionalOp {
    @Specialization
    public static void markFrameAsNoLongerOnStack(@SuppressWarnings("unused") final VirtualFrame frame,
        @SuppressWarnings("unused") final AbstractTruffleException ex,
        @Bind SomOperations root) {
      if (root.frameOnStackMarkerIdx != -1) {
        var marker = (FrameOnStackMarker) frame.getObject(root.frameOnStackMarkerIdx);
        marker.frameNoLongerOnStack();
      }

      throw ex;
    }
  }

  @EpilogReturn
  public static final class ExecuteEpilogOp {
    @Specialization
    public static Object markFrameAsNoLongerOnStack(@SuppressWarnings("unused") final VirtualFrame frame,
        Object object, @Bind SomOperations root) {
      if (root.frameOnStackMarkerIdx != -1) {
        FrameOnStackMarker marker =
            (FrameOnStackMarker) frame.getObject(root.frameOnStackMarkerIdx);
        marker.frameNoLongerOnStack();
      }
      return object;
    }

  }

  @Override
  public Object interceptControlFlowException(ControlFlowException ex, VirtualFrame frame,
      BytecodeNode bytecodeNode, int bci) throws Throwable {
    if (frameOnStackMarkerIdx != -1) {
      var marker = (FrameOnStackMarker) frame.getObject(frameOnStackMarkerIdx);
      marker.frameNoLongerOnStack();
      if (ex instanceof ReturnException retEx && retEx.reachedTarget(marker)) {
        return retEx.result();
      }
    }

    throw ex;
  }

  @Override
  public void propagateLoopCountThroughoutLexicalScope(final long count) {
    assert count >= 0;
    // TODO:
    // currentLexicalScope.propagateLoopCountThroughoutLexicalScope(count);
    LoopNode.reportLoopCount(this,
        (count > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) count);
  }
}
