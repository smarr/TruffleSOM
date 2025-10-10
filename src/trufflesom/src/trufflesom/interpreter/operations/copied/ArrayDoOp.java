package trufflesom.interpreter.operations.copied;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.bytecode.OperationProxy.Proxyable;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

import trufflesom.interpreter.Invokable;
import trufflesom.interpreter.Types;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.nary.BinaryExpressionNode;
import trufflesom.interpreter.operations.SomOperations;
import trufflesom.primitives.basics.BlockPrims.ValueOnePrim;
import trufflesom.primitives.basics.BlockPrimsFactory.ValueOnePrimFactory;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SArray.PartiallyEmptyArray;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


@Proxyable
@ImportStatic(Types.class)
public abstract class ArrayDoOp extends BinaryExpressionNode {

  public static final SSymbol symDo = SymbolTable.symbolFor("do:");

  @NeverDefault
  public static ValueOnePrim createValue() {
    return ValueOnePrimFactory.create(null, null);
  }

  @Specialization(guards = "arr.isEmptyType()")
  public static final SArray doEmptyArray(final VirtualFrame frame,
      final SArray arr, final SBlock block,
      @Bind final Node node,
      @Shared("valuePrim") @Cached("createValue()") final ValueOnePrim valuePrim) {
    int length = arr.getEmptyStorage();
    try {
      if (SArray.FIRST_IDX < length) {
        valuePrim.executeEvaluated(frame, block, Nil.nilObject);
      }
      for (long i = SArray.FIRST_IDX + 1; i < length; i++) {
        valuePrim.executeEvaluated(frame, block, Nil.nilObject);
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount(node, length);
      }
    }
    return arr;
  }

  @Specialization(guards = "arr.isPartiallyEmptyType()")
  public static final SArray doPartiallyEmptyArray(final VirtualFrame frame,
      final SArray arr, final SBlock block,
      @Bind final Node node,
      @Shared("valuePrim") @Cached("createValue()") final ValueOnePrim valuePrim) {
    PartiallyEmptyArray storage = arr.getPartiallyEmptyStorage();
    int length = storage.getLength();
    try {
      if (SArray.FIRST_IDX < length) {
        valuePrim.executeEvaluated(frame, block, storage.get(SArray.FIRST_IDX));
      }
      for (long i = SArray.FIRST_IDX + 1; i < length; i++) {
        valuePrim.executeEvaluated(frame, block, storage.get(i));
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount(node, length);
      }
    }
    return arr;
  }

  @Specialization(guards = "arr.isObjectType()")
  public static final SArray doObjectArray(final VirtualFrame frame,
      final SArray arr, final SBlock block,
      @Bind final Node node,
      @Shared("valuePrim") @Cached("createValue()") final ValueOnePrim valuePrim) {
    Object[] storage = arr.getObjectStorage();
    int length = storage.length;
    try {
      if (SArray.FIRST_IDX < length) {
        valuePrim.executeEvaluated(frame, block, storage[SArray.FIRST_IDX]);
      }
      for (long i = SArray.FIRST_IDX + 1; i < length; i++) {
        valuePrim.executeEvaluated(frame, block, storage[(int) i]);
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount(node, length);
      }
    }
    return arr;
  }

  @Specialization(guards = "arr.isLongType()")
  public static final SArray doLongArray(final VirtualFrame frame,
      final SArray arr, final SBlock block,
      @Bind final Node node,
      @Shared("valuePrim") @Cached("createValue()") final ValueOnePrim valuePrim) {
    long[] storage = arr.getLongStorage();
    int length = storage.length;
    try {
      if (SArray.FIRST_IDX < length) {
        valuePrim.executeEvaluated(frame, block, storage[SArray.FIRST_IDX]);
      }
      for (long i = SArray.FIRST_IDX + 1; i < length; i++) {
        valuePrim.executeEvaluated(frame, block, storage[(int) i]);
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount(node, length);
      }
    }
    return arr;
  }

  @Specialization(guards = "arr.isDoubleType()")
  public static final SArray doDoubleArray(final VirtualFrame frame,
      final SArray arr, final SBlock block,
      @Bind final Node node,
      @Shared("valuePrim") @Cached("createValue()") final ValueOnePrim valuePrim) {
    double[] storage = arr.getDoubleStorage();
    int length = storage.length;
    try {
      if (SArray.FIRST_IDX < length) {
        valuePrim.executeEvaluated(frame, block, storage[SArray.FIRST_IDX]);
      }
      for (long i = SArray.FIRST_IDX + 1; i < length; i++) {
        valuePrim.executeEvaluated(frame, block, storage[(int) i]);
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount(node, length);
      }
    }
    return arr;
  }

  @Specialization(guards = "arr.isBooleanType()")
  public static final SArray doBooleanArray(final VirtualFrame frame,
      final SArray arr, final SBlock block,
      @Bind final Node node,
      @Shared("valuePrim") @Cached("createValue()") final ValueOnePrim valuePrim) {
    boolean[] storage = arr.getBooleanStorage();
    int length = storage.length;
    try {
      if (SArray.FIRST_IDX < length) {
        valuePrim.executeEvaluated(frame, block, storage[SArray.FIRST_IDX]);
      }
      for (long i = SArray.FIRST_IDX + 1; i < length; i++) {
        valuePrim.executeEvaluated(frame, block, storage[(int) i]);
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount(node, length);
      }
    }
    return arr;
  }

  @Specialization
  public static Object fallback(final VirtualFrame frame, final SObject rcvr,
      final Object arg,
      @Cached("create(symDo)") final AbstractDispatchNode dispatch) {
    return dispatch.executeDispatch(frame, new Object[] {rcvr, arg});
  }

  public static final void reportLoopCount(final Node node, final long count) {
    if (count == 0) {
      return;
    }

    CompilerAsserts.neverPartOfCompilation("reportLoopCount");
    Node current = node.getParent();
    while (current != null && !(current instanceof RootNode)) {
      current = current.getParent();
    }
    if (current != null) {
        if (current instanceof Invokable i) {
            i.propagateLoopCountThroughoutLexicalScope(count);
        } else {
            ((SomOperations) current).propagateLoopCountThroughoutLexicalScope(count);
        }
    }
  }
}
