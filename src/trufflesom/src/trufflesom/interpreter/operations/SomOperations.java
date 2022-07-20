package trufflesom.interpreter.operations;

import java.util.HashMap;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.bytecode.BytecodeLocal;
import com.oracle.truffle.api.bytecode.BytecodeRootNode;
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
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.SomLanguage;
import trufflesom.interpreter.Types;
import trufflesom.interpreter.nodes.ContextualNode;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.specialized.AndBoolMessageNode;
import trufflesom.interpreter.nodes.specialized.IntIncrementNode;
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
import trufflesom.interpreter.operations.copied.ReturnNonLocal;
import trufflesom.interpreter.operations.copied.SubtractionOp;
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
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SAbstractObject;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


@GenerateBytecode(languageClass = SomLanguage.class)
@TypeSystemReference(Types.class)
@OperationProxy(SubtractionOp.class)
@OperationProxy(AdditionOp.class)
@OperationProxy(MultiplicationPrim.class)
@OperationProxy(ReturnNonLocal.class)
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
public abstract class SomOperations extends RootNode implements BytecodeRootNode {

  protected SomOperations(final TruffleLanguage<?> language,
      final FrameDescriptor.Builder frameDescriptorBuilder) {
    super(language, makeDescriptorWithNil(frameDescriptorBuilder));
  }

  private static FrameDescriptor makeDescriptorWithNil(final FrameDescriptor.Builder builder) {
    builder.defaultValue(Nil.nilObject);
    return builder.build();
  }

  public static final class LexicalScopeForOp {
    public final HashMap<Local, BytecodeLocal> opLocals;
    public final LexicalScopeForOp             outer;

    public LexicalScopeForOp(final HashMap<Local, BytecodeLocal> opLocals,
        final LexicalScopeForOp outer) {
      this.opLocals = opLocals;
      this.outer = outer;
    }
  }

  @Operation
  public static final class DetermineContextOp {

    @Specialization
    public static MaterializedFrame determineContext(final VirtualFrame frame,
        final int contextLevel) {
      return ContextualNode.determineContext(frame, contextLevel);
    }
  }

  @Operation
  @ImportStatic(Types.class)
  @TypeSystemReference(Types.class)
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
  @TypeSystemReference(Types.class)
  public static final class SuperSendOp {
    @Specialization
    public static Object doSuper(final DirectCallNode callNode,
        @Variadic final Object[] arguments) {
      return callNode.call(arguments);
    }
  }

  @Operation
  @ImportStatic(Globals.class)
  @TypeSystemReference(Types.class)
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
  @TypeSystemReference(Types.class)
  public static final class GlobalCachedReadOp {
    @NeverDefault
    public static Assumption get(final Object assoc) {
      return ((Association) assoc).getAssumption();
    }

    @Specialization // TODO: debug DSL bug: (assumptions = "cachedAssumption", limit = "1")
    public static Object doCached(final Object assoc,
        @Cached("get(assoc)") final Assumption cachedAssumption) {
      return ((Association) assoc).getValue();
    }
  }

  @NeverDefault
  protected static SClass getBlockClass(final SMethod blockMethod) {
    return Classes.getBlockClass(blockMethod.getNumberOfArguments());
  }

  @Operation
  @TypeSystemReference(Types.class)
  @ImportStatic(SomOperations.class)
  public static final class PushBlockWithoutContextOp {
    @Specialization
    public static SBlock instantiate(final SMethod blockMethod,
        @Cached("getBlockClass(blockMethod)") final SClass blockClass) {
      return new SBlock(blockMethod, blockClass, null);
    }
  }

  @Operation
  @TypeSystemReference(Types.class)
  @ImportStatic(SomOperations.class)
  public static final class PushBlockWithContextOp {
    @Specialization
    public static SBlock instantiate(final VirtualFrame frame, final SMethod blockMethod,
        @Cached("getBlockClass(blockMethod)") final SClass blockClass) {
      return new SBlock(blockMethod, blockClass, frame.materialize());
    }
  }

  @Operation
  @TypeSystemReference(Types.class)
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
  @TypeSystemReference(Types.class)
  public static final class UnsafeLoopIncrement {
    @Specialization
    public static void increment(final VirtualFrame frame, final long currentValue,
        final LocalSetter setter) {
      setter.setLong(frame, currentValue + 1);
    }

    @Specialization
    public static void increment(final VirtualFrame frame, final double currentValue,
        final LocalSetter setter) {
      setter.setDouble(frame, currentValue + 1.0);
    }
  }

  @Operation
  @TypeSystemReference(Types.class)
  public static final class UnsafeLoopDecrement {
    @Specialization
    public static void increment(final VirtualFrame frame, final long currentValue,
        final LocalSetter setter) {
      setter.setLong(frame, currentValue - 1);
    }

    @Specialization
    public static void increment(final VirtualFrame frame, final double currentValue,
        final LocalSetter setter) {
      setter.setDouble(frame, currentValue - 1.0);
    }
  }

  @Operation
  @TypeSystemReference(Types.class)
  @ImportStatic(FieldAccessorNode.class)
  public static final class ReadField {
    @Specialization(rewriteOn = UnexpectedResultException.class)
    public static long readLong(@SuppressWarnings("unused") final SObject self,
        @SuppressWarnings("unused") final int fieldIdx,
        @Cached("createRead(fieldIdx)") final AbstractReadFieldNode read)
        throws UnexpectedResultException {
      return read.readLong(self);
    }

    @Specialization(rewriteOn = UnexpectedResultException.class)
    public static double readDouble(@SuppressWarnings("unused") final SObject self,
        @SuppressWarnings("unused") final int fieldIdx,
        @Cached("createRead(fieldIdx)") final AbstractReadFieldNode read)
        throws UnexpectedResultException {
      return read.readDouble(self);
    }

    @Specialization
    public static Object readObject(@SuppressWarnings("unused") final SObject self,
        @SuppressWarnings("unused") final int fieldIdx,
        @Cached("createRead(fieldIdx)") final AbstractReadFieldNode read) {
      return read.read(self);
    }
  }

  @Operation
  @TypeSystemReference(Types.class)
  @ImportStatic(FieldAccessorNode.class)
  public static final class WriteField {
    @Specialization
    public static long writeLong(final SObject self, final long value,
        @SuppressWarnings("unused") final int fieldIdx,
        @Cached("createWrite(fieldIdx)") final AbstractWriteFieldNode write) {
      return write.write(self, value);
    }

    @Specialization
    public static double writeDouble(final SObject self, final double value,
        @SuppressWarnings("unused") final int fieldIdx,
        @Cached("createWrite(fieldIdx)") final AbstractWriteFieldNode write) {
      return write.write(self, value);
    }

    @Specialization
    public static Object writeObject(final SObject self, final Object value,
        @SuppressWarnings("unused") final int fieldIdx,
        @Cached("createWrite(fieldIdx)") final AbstractWriteFieldNode write) {
      return write.write(self, value);
    }
  }

  @Operation
  @TypeSystemReference(Types.class)
  @ImportStatic(FieldAccessorNode.class)
  public static final class WriteFieldAndReturnSelf {
    @Specialization
    public static SObject writeLong(final SObject self, final long value,
        @SuppressWarnings("unused") final int fieldIdx,
        @Cached("createWrite(fieldIdx)") final AbstractWriteFieldNode write) {
      write.write(self, value);
      return self;
    }

    @Specialization
    public static SObject writeDouble(final SObject self, final double value,
        @SuppressWarnings("unused") final int fieldIdx,
        @Cached("createWrite(fieldIdx)") final AbstractWriteFieldNode write) {
      write.write(self, value);
      return self;
    }

    @Specialization
    public static SObject writeObject(final SObject self, final Object value,
        @SuppressWarnings("unused") final int fieldIdx,
        @Cached("createWrite(fieldIdx)") final AbstractWriteFieldNode write) {
      write.write(self, value);
      return self;
    }
  }

  @Operation
  @TypeSystemReference(Types.class)
  @ImportStatic(FieldAccessorNode.class)
  public static final class IncField {
    @Specialization
    public static long incLong(final SObject self,
        @SuppressWarnings("unused") final int fieldIdx,
        @Cached("createIncrement(fieldIdx, self)") final IncrementLongFieldNode inc) {
      return inc.increment(self);
    }
  }

  @Operation
  @TypeSystemReference(Types.class)
  public static final class WriteArgument {
    @Specialization
    public static Object writeArg(final VirtualFrame frame, final Object value,
        final int argIdx) {
      frame.getArguments()[argIdx] = value;
      return value;
    }
  }

  public void propagateLoopCountThroughoutLexicalScope(final long count) {
    assert count >= 0;
    // TODO:
    // currentLexicalScope.propagateLoopCountThroughoutLexicalScope(count);
    LoopNode.reportLoopCount(this,
        (count > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) count);
  }
}
