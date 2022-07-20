package trufflesom.interpreter.operations.copied;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.bytecode.OperationProxy.Proxyable;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

import trufflesom.interpreter.Invokable;
import trufflesom.interpreter.nodes.nary.BinaryExpressionNode;
import trufflesom.primitives.basics.BlockPrims.ValueNonePrim;
import trufflesom.primitives.basics.BlockPrimsFactory.ValueNonePrimFactory;
import trufflesom.primitives.basics.LengthPrim;
import trufflesom.primitives.basics.LengthPrimFactory;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SObject;


@Proxyable
public abstract class ArrayPutAllOp extends BinaryExpressionNode {
  public static final ValueNonePrim create() {
    return ValueNonePrimFactory.create(null);
  }

  @NeverDefault
  public static final LengthPrim createLength() {
    return LengthPrimFactory.create(null);
  }

  public static final boolean valueIsNil(final SObject value) {
    return value == Nil.nilObject;
  }

  public static final boolean valueOfNoOtherSpecialization(final Object value) {
    return !(value instanceof Long) &&
        !(value instanceof Double) &&
        !(value instanceof Boolean) &&
        !(value instanceof SBlock);
  }

  @Specialization(guards = {"rcvr.isEmptyType()", "valueIsNil(nil)"})
  public static SArray doPutNilInEmptyArray(final SArray rcvr,
      @SuppressWarnings("unused") final SObject nil,
      @SuppressWarnings("unused") @Shared("length") @Cached("createLength()") final LengthPrim length) {
    // NO OP
    return rcvr;
  }

  @Specialization(guards = {"valueIsNil(nil)"}, replaces = {"doPutNilInEmptyArray"})
  public static SArray doPutNilInOtherArray(final SArray rcvr,
      @SuppressWarnings("unused") final SObject nil,
      @Shared("length") @Cached("createLength()") final LengthPrim length) {
    rcvr.transitionToEmpty(length.executeEvaluated(null, rcvr));
    return rcvr;
  }

  private static void evalBlockForRemaining(final VirtualFrame frame,
      final SBlock block, final long length, final Object[] storage,
      final ValueNonePrim call) {
    for (int i = SArray.FIRST_IDX + 1; i < length; i++) {
      storage[i] = call.executeEvaluated(frame, block);
    }
  }

  private static void evalBlockForRemaining(final VirtualFrame frame,
      final SBlock block, final long length, final long[] storage, final ValueNonePrim call) {
    for (int i = SArray.FIRST_IDX + 1; i < length; i++) {
      storage[i] = (long) call.executeEvaluated(frame, block);
    }
  }

  private static void evalBlockForRemaining(final VirtualFrame frame,
      final SBlock block, final long length, final double[] storage,
      final ValueNonePrim call) {
    for (int i = SArray.FIRST_IDX + 1; i < length; i++) {
      storage[i] = (double) call.executeEvaluated(frame, block);
    }
  }

  private static void evalBlockForRemaining(final VirtualFrame frame,
      final SBlock block, final long length, final boolean[] storage,
      final ValueNonePrim call) {
    for (int i = SArray.FIRST_IDX + 1; i < length; i++) {
      storage[i] = (boolean) call.executeEvaluated(frame, block);
    }
  }

  @Specialization
  public static SArray doPutEvalBlock(final VirtualFrame frame, final SArray rcvr,
      final SBlock block,
      @Shared("length") @Cached("createLength()") final LengthPrim lengthNode,
      @Bind("this") final Node node,
      @Cached final ValueNonePrim call) {
    int length = (int) lengthNode.executeEvaluated(frame, rcvr);
    if (length <= 0) {
      return rcvr;
    }
    // TODO: this version does not handle the case that a subsequent value is not of the
    // expected type...
    try {
      Object result = call.executeEvaluated(frame, block);
      if (result instanceof Long) {
        long[] newStorage = new long[length];
        newStorage[0] = (long) result;
        evalBlockForRemaining(frame, block, length, newStorage, call);
        rcvr.transitionTo(newStorage);
      } else if (result instanceof Double) {
        double[] newStorage = new double[length];
        newStorage[0] = (double) result;
        evalBlockForRemaining(frame, block, length, newStorage, call);
        rcvr.transitionTo(newStorage);
      } else if (result instanceof Boolean) {
        boolean[] newStorage = new boolean[length];
        newStorage[0] = (boolean) result;
        evalBlockForRemaining(frame, block, length, newStorage, call);
        rcvr.transitionTo(newStorage);
      } else {
        Object[] newStorage = new Object[length];
        newStorage[0] = result;
        evalBlockForRemaining(frame, block, length, newStorage, call);
        rcvr.transitionTo(newStorage);
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        ArrayDoOp.reportLoopCount(node, length);
      }
    }
    return rcvr;
  }

  protected final void reportLoopCount(final long count) {
    if (count == 0) {
      return;
    }

    CompilerAsserts.neverPartOfCompilation("reportLoopCount");
    Node current = getParent();
    while (current != null && !(current instanceof RootNode)) {
      current = current.getParent();
    }
    if (current != null) {
      ((Invokable) current).propagateLoopCountThroughoutLexicalScope(count);
    }
  }

  @Specialization
  public static SArray doPutLong(final SArray rcvr, final long value,
      @Shared("length") @Cached("createLength()") final LengthPrim length) {
    rcvr.transitionToLongWithAll(length.executeEvaluated(null, rcvr), value);
    return rcvr;
  }

  @Specialization
  public static SArray doPutDouble(final SArray rcvr, final double value,
      @Shared("length") @Cached("createLength()") final LengthPrim length) {
    rcvr.transitionToDoubleWithAll(length.executeEvaluated(null, rcvr), value);
    return rcvr;
  }

  @Specialization
  public static SArray doPutBoolean(final SArray rcvr, final boolean value,
      @Shared("length") @Cached("createLength()") final LengthPrim length) {
    rcvr.transitionToBooleanWithAll(length.executeEvaluated(null, rcvr), value);
    return rcvr;
  }

  @Specialization(guards = {"valueOfNoOtherSpecialization(value)"})
  public static SArray doPutObject(final SArray rcvr, final Object value,
      @Shared("length") @Cached("createLength()") final LengthPrim length) {
    rcvr.transitionToObjectWithAll(length.executeEvaluated(null, rcvr), value);
    return rcvr;
  }

  @Fallback
  public static Object makeGenericSend(final VirtualFrame frame, final Object rcvr,
      final Object value,
      @Bind("this") final Node node) {
    return ((ArrayPutAllOp) node).makeGenericSend(
        SymbolTable.symbolFor("putAll:")).doPreEvaluated(
            frame, new Object[] {rcvr, value});
  }
}
