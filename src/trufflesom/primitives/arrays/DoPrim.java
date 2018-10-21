package som.primitives.arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.api.source.SourceSection;

import som.interpreter.Invokable;
import som.interpreter.nodes.dispatch.AbstractDispatchNode;
import som.interpreter.nodes.dispatch.UninitializedValuePrimDispatchNode;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.primitives.Primitive;
import som.primitives.basics.BlockPrims.ValuePrimitiveNode;
import som.vm.constants.Nil;
import som.vmobjects.SArray;
import som.vmobjects.SArray.ArrayType;
import som.vmobjects.SArray.PartiallyEmptyArray;
import som.vmobjects.SBlock;


@GenerateNodeFactory
@ImportStatic(ArrayType.class)
@Primitive(className = "Array", primitive = "do:", selector = "do:",
    receiverType = SArray.class, disabled = true)
public abstract class DoPrim extends BinaryExpressionNode
    implements ValuePrimitiveNode {
  @Child private AbstractDispatchNode block;
  private final ValueProfile          storageType = ValueProfile.createClassProfile();

  public DoPrim(final SourceSection source) {
    super(source);
    block = new UninitializedValuePrimDispatchNode();
  }

  @Override
  public void adoptNewDispatchListHead(final AbstractDispatchNode node) {
    block = insert(node);
  }

  private void execBlock(final VirtualFrame frame, final SBlock block, final Object arg) {
    this.block.executeDispatch(frame, new Object[] {block, arg});
  }

  @Specialization(guards = "isEmptyType(arr)")
  public final SArray doEmptyArray(final VirtualFrame frame,
      final SArray arr, final SBlock block) {
    int length = arr.getEmptyStorage(storageType);
    try {
      if (SArray.FIRST_IDX < length) {
        execBlock(frame, block, Nil.nilObject);
      }
      for (long i = SArray.FIRST_IDX + 1; i < length; i++) {
        execBlock(frame, block, Nil.nilObject);
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount(length);
      }
    }
    return arr;
  }

  @Specialization(guards = "isPartiallyEmptyType(arr)")
  public final SArray doPartiallyEmptyArray(final VirtualFrame frame,
      final SArray arr, final SBlock block) {
    PartiallyEmptyArray storage = arr.getPartiallyEmptyStorage(storageType);
    int length = storage.getLength();
    try {
      if (SArray.FIRST_IDX < length) {
        execBlock(frame, block, storage.get(SArray.FIRST_IDX));
      }
      for (long i = SArray.FIRST_IDX + 1; i < length; i++) {
        execBlock(frame, block, storage.get(i));
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount(length);
      }
    }
    return arr;
  }

  @Specialization(guards = "isObjectType(arr)")
  public final SArray doObjectArray(final VirtualFrame frame,
      final SArray arr, final SBlock block) {
    Object[] storage = arr.getObjectStorage(storageType);
    int length = storage.length;
    try {
      if (SArray.FIRST_IDX < length) {
        execBlock(frame, block, storage[SArray.FIRST_IDX]);
      }
      for (long i = SArray.FIRST_IDX + 1; i < length; i++) {
        execBlock(frame, block, storage[(int) i]);
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount(length);
      }
    }
    return arr;
  }

  @Specialization(guards = "isLongType(arr)")
  public final SArray doLongArray(final VirtualFrame frame,
      final SArray arr, final SBlock block) {
    long[] storage = arr.getLongStorage(storageType);
    int length = storage.length;
    try {
      if (SArray.FIRST_IDX < length) {
        execBlock(frame, block, storage[SArray.FIRST_IDX]);
      }
      for (long i = SArray.FIRST_IDX + 1; i < length; i++) {
        execBlock(frame, block, storage[(int) i]);
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount(length);
      }
    }
    return arr;
  }

  @Specialization(guards = "isDoubleType(arr)")
  public final SArray doDoubleArray(final VirtualFrame frame,
      final SArray arr, final SBlock block) {
    double[] storage = arr.getDoubleStorage(storageType);
    int length = storage.length;
    try {
      if (SArray.FIRST_IDX < length) {
        execBlock(frame, block, storage[SArray.FIRST_IDX]);
      }
      for (long i = SArray.FIRST_IDX + 1; i < length; i++) {
        execBlock(frame, block, storage[(int) i]);
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount(length);
      }
    }
    return arr;
  }

  @Specialization(guards = "isBooleanType(arr)")
  public final SArray doBooleanArray(final VirtualFrame frame,
      final SArray arr, final SBlock block) {
    boolean[] storage = arr.getBooleanStorage(storageType);
    int length = storage.length;
    try {
      if (SArray.FIRST_IDX < length) {
        execBlock(frame, block, storage[SArray.FIRST_IDX]);
      }
      for (long i = SArray.FIRST_IDX + 1; i < length; i++) {
        execBlock(frame, block, storage[(int) i]);
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
