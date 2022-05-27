package trufflesom.primitives.arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

import bdt.primitives.Primitive;
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


@GenerateNodeFactory
@Primitive(className = "Array", primitive = "putAll:", selector = "putAll:", disabled = true,
    extraChild = LengthPrimFactory.class)
@NodeChild(value = "length", type = LengthPrim.class, executeWith = "receiver")
public abstract class PutAllNode extends BinaryExpressionNode {
  @Child private ValueNonePrim block = ValueNonePrimFactory.create(null);

  protected static final boolean valueIsNil(final SObject value) {
    return value == Nil.nilObject;
  }

  protected static final boolean valueOfNoOtherSpecialization(final Object value) {
    return !(value instanceof Long) &&
        !(value instanceof Double) &&
        !(value instanceof Boolean) &&
        !(value instanceof SBlock);
  }

  @Specialization(guards = {"rcvr.isEmptyType()", "valueIsNil(nil)"})
  public SArray doPutNilInEmptyArray(final SArray rcvr, final SObject nil,
      final long length) {
    // NO OP
    return rcvr;
  }

  @Specialization(guards = {"valueIsNil(nil)"}, replaces = {"doPutNilInEmptyArray"})
  public SArray doPutNilInOtherArray(final SArray rcvr, final SObject nil,
      final long length) {
    rcvr.transitionToEmpty(length);
    return rcvr;
  }

  private void evalBlockForRemaining(final VirtualFrame frame,
      final SBlock block, final long length, final Object[] storage) {
    for (int i = SArray.FIRST_IDX + 1; i < length; i++) {
      storage[i] = this.block.executeEvaluated(frame, block);
    }
  }

  private void evalBlockForRemaining(final VirtualFrame frame,
      final SBlock block, final long length, final long[] storage) {
    for (int i = SArray.FIRST_IDX + 1; i < length; i++) {
      storage[i] = (long) this.block.executeEvaluated(frame, block);
    }
  }

  private void evalBlockForRemaining(final VirtualFrame frame,
      final SBlock block, final long length, final double[] storage) {
    for (int i = SArray.FIRST_IDX + 1; i < length; i++) {
      storage[i] = (double) this.block.executeEvaluated(frame, block);
    }
  }

  private void evalBlockForRemaining(final VirtualFrame frame,
      final SBlock block, final long length, final boolean[] storage) {
    for (int i = SArray.FIRST_IDX + 1; i < length; i++) {
      storage[i] = (boolean) this.block.executeEvaluated(frame, block);
    }
  }

  @Specialization
  public SArray doPutEvalBlock(final VirtualFrame frame, final SArray rcvr,
      final SBlock block, final long length) {
    if (length <= 0) {
      return rcvr;
    }
    // TODO: this version does not handle the case that a subsequent value is not of the
    // expected type...
    try {
      Object result = this.block.executeEvaluated(frame, block);
      if (result instanceof Long) {
        long[] newStorage = new long[(int) length];
        newStorage[0] = (long) result;
        evalBlockForRemaining(frame, block, length, newStorage);
        rcvr.transitionTo(newStorage);
      } else if (result instanceof Double) {
        double[] newStorage = new double[(int) length];
        newStorage[0] = (double) result;
        evalBlockForRemaining(frame, block, length, newStorage);
        rcvr.transitionTo(newStorage);
      } else if (result instanceof Boolean) {
        boolean[] newStorage = new boolean[(int) length];
        newStorage[0] = (boolean) result;
        evalBlockForRemaining(frame, block, length, newStorage);
        rcvr.transitionTo(newStorage);
      } else {
        Object[] newStorage = new Object[(int) length];
        newStorage[0] = result;
        evalBlockForRemaining(frame, block, length, newStorage);
        rcvr.transitionTo(newStorage);
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount(length);
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
  public SArray doPutLong(final SArray rcvr, final long value,
      final long length) {
    rcvr.transitionToLongWithAll(length, value);
    return rcvr;
  }

  @Specialization
  public SArray doPutDouble(final SArray rcvr, final double value,
      final long length) {
    rcvr.transitionToDoubleWithAll(length, value);
    return rcvr;
  }

  @Specialization
  public SArray doPutBoolean(final SArray rcvr, final boolean value,
      final long length) {
    rcvr.transitionToBooleanWithAll(length, value);
    return rcvr;
  }

  @Specialization(guards = {"valueOfNoOtherSpecialization(value)"})
  public SArray doPutObject(final SArray rcvr, final Object value,
      final long length) {
    rcvr.transitionToObjectWithAll(length, value);
    return rcvr;
  }

  @Fallback
  public Object makeGenericSend(final VirtualFrame frame, final Object rcvr,
      final Object value, final Object length) {
    return makeGenericSend(SymbolTable.symbolFor("putAll:")).doPreEvaluated(frame,
        new Object[] {rcvr, value});
  }
}
