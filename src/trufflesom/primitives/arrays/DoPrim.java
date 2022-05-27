package trufflesom.primitives.arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

import bdt.primitives.Primitive;
import trufflesom.interpreter.Invokable;
import trufflesom.interpreter.nodes.nary.BinaryMsgExprNode;
import trufflesom.primitives.basics.BlockPrims.ValueOnePrim;
import trufflesom.primitives.basics.BlockPrimsFactory.ValueOnePrimFactory;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SArray.PartiallyEmptyArray;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SSymbol;


@GenerateNodeFactory
@Primitive(className = "Array", primitive = "do:", selector = "do:",
    receiverType = SArray.class, disabled = true)
public abstract class DoPrim extends BinaryMsgExprNode {
  @Child private ValueOnePrim block = ValueOnePrimFactory.create(null, null);

  @Override
  public SSymbol getSelector() {
    return SymbolTable.symbolFor("do:");
  }

  @Specialization(guards = "arr.isEmptyType()")
  public final SArray doEmptyArray(final VirtualFrame frame,
      final SArray arr, final SBlock block) {
    int length = arr.getEmptyStorage();
    try {
      if (SArray.FIRST_IDX < length) {
        this.block.executeEvaluated(frame, block, Nil.nilObject);
      }
      for (long i = SArray.FIRST_IDX + 1; i < length; i++) {
        this.block.executeEvaluated(frame, block, Nil.nilObject);
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount(length);
      }
    }
    return arr;
  }

  @Specialization(guards = "arr.isPartiallyEmptyType()")
  public final SArray doPartiallyEmptyArray(final VirtualFrame frame,
      final SArray arr, final SBlock block) {
    PartiallyEmptyArray storage = arr.getPartiallyEmptyStorage();
    int length = storage.getLength();
    try {
      if (SArray.FIRST_IDX < length) {
        this.block.executeEvaluated(frame, block, storage.get(SArray.FIRST_IDX));
      }
      for (long i = SArray.FIRST_IDX + 1; i < length; i++) {
        this.block.executeEvaluated(frame, block, storage.get(i));
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount(length);
      }
    }
    return arr;
  }

  @Specialization(guards = "arr.isObjectType()")
  public final SArray doObjectArray(final VirtualFrame frame,
      final SArray arr, final SBlock block) {
    Object[] storage = arr.getObjectStorage();
    int length = storage.length;
    try {
      if (SArray.FIRST_IDX < length) {
        this.block.executeEvaluated(frame, block, storage[SArray.FIRST_IDX]);
      }
      for (long i = SArray.FIRST_IDX + 1; i < length; i++) {
        this.block.executeEvaluated(frame, block, storage[(int) i]);
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount(length);
      }
    }
    return arr;
  }

  @Specialization(guards = "arr.isLongType()")
  public final SArray doLongArray(final VirtualFrame frame,
      final SArray arr, final SBlock block) {
    long[] storage = arr.getLongStorage();
    int length = storage.length;
    try {
      if (SArray.FIRST_IDX < length) {
        this.block.executeEvaluated(frame, block, storage[SArray.FIRST_IDX]);
      }
      for (long i = SArray.FIRST_IDX + 1; i < length; i++) {
        this.block.executeEvaluated(frame, block, storage[(int) i]);
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount(length);
      }
    }
    return arr;
  }

  @Specialization(guards = "arr.isDoubleType()")
  public final SArray doDoubleArray(final VirtualFrame frame,
      final SArray arr, final SBlock block) {
    double[] storage = arr.getDoubleStorage();
    int length = storage.length;
    try {
      if (SArray.FIRST_IDX < length) {
        this.block.executeEvaluated(frame, block, storage[SArray.FIRST_IDX]);
      }
      for (long i = SArray.FIRST_IDX + 1; i < length; i++) {
        this.block.executeEvaluated(frame, block, storage[(int) i]);
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount(length);
      }
    }
    return arr;
  }

  @Specialization(guards = "arr.isBooleanType()")
  public final SArray doBooleanArray(final VirtualFrame frame,
      final SArray arr, final SBlock block) {
    boolean[] storage = arr.getBooleanStorage();
    int length = storage.length;
    try {
      if (SArray.FIRST_IDX < length) {
        this.block.executeEvaluated(frame, block, storage[SArray.FIRST_IDX]);
      }
      for (long i = SArray.FIRST_IDX + 1; i < length; i++) {
        this.block.executeEvaluated(frame, block, storage[(int) i]);
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount(length);
      }
    }
    return arr;
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
}
