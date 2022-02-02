package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.AbstractInvokable;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;
import trufflesom.primitives.arrays.AtPrim;
import trufflesom.primitives.arrays.AtPrimFactory;
import trufflesom.primitives.arrays.AtPutPrim;
import trufflesom.primitives.arrays.AtPutPrimFactory;
import trufflesom.primitives.arrays.DoIndexesPrim;
import trufflesom.primitives.arrays.DoIndexesPrimFactory;
import trufflesom.primitives.arrays.NewPrim;
import trufflesom.primitives.arrays.NewPrimFactory;
import trufflesom.primitives.basics.LengthPrim;
import trufflesom.primitives.basics.LengthPrimFactory;
import trufflesom.primitives.basics.NewObjectPrim;
import trufflesom.primitives.basics.NewObjectPrimFactory;
import trufflesom.vm.Classes;
import trufflesom.vm.NotYetImplementedException;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SObject;


public abstract class VectorClass {
  /**
   * <pre>
   * new: initialSize = ( ^ super new initialize: initialSize. )
   * </pre>
   */
  public static final class VectorNew2 extends AbstractInvokable {

    @Child private NewObjectPrim        newPrim;
    @Child private AbstractDispatchNode dispatchInit;

    public VectorNew2(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      newPrim = NewObjectPrimFactory.create(null);
      dispatchInit = new UninitializedDispatchNode(SymbolTable.symbolFor("initialize:"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SClass clazz = (SClass) args[0];
      Object initialSize = args[1];

      Object newObj = newPrim.executeEvaluated(frame, clazz);
      return dispatchInit.executeDispatch(frame, new Object[] {newObj, initialSize});
    }
  }

  /**
   * <pre>
   * initialize: size = (
       first := 1.
       last  := 1.
       storage := Array new: size.
     )
   * </pre>
   */
  public static final class VectorInitialize extends AbstractInvokable {

    @Child private AbstractWriteFieldNode writeFirst;
    @Child private AbstractWriteFieldNode writeLast;
    @Child private AbstractWriteFieldNode writeStorage;

    @Child private NewPrim newPrim;

    public VectorInitialize(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      writeFirst = FieldAccessorNode.createWrite(0);
      writeLast = FieldAccessorNode.createWrite(1);
      writeStorage = FieldAccessorNode.createWrite(2);

      newPrim = NewPrimFactory.create(null, null);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      Object size = args[1];

      writeFirst.write(rcvr, 1L);
      writeLast.write(rcvr, 1L);

      Object newArr = newPrim.executeEvaluated(frame, Classes.arrayClass, size);

      writeStorage.write(rcvr, newArr);
      return rcvr;
    }
  }

  /**
   * <pre>
   * size = ( ^last - first ).
   * </pre>
   */
  public static final class VectorSize extends AbstractInvokable {

    @Child private AbstractReadFieldNode readFirst;
    @Child private AbstractReadFieldNode readLast;

    public VectorSize(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      readFirst = FieldAccessorNode.createRead(0);
      readLast = FieldAccessorNode.createRead(1);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];

      return readLast.readLongSafe(rcvr) - readFirst.readLongSafe(rcvr);
    }
  }

  /**
   * <pre>
   * at: index = (
   * index > storage length ifTrue: [ ^ nil ].
   * ^ storage at: index
   * )
   */
  public static final class VectorAt extends AbstractInvokable {

    @Child private AtPrim                atPrim;
    @Child private LengthPrim            lengthPrim;
    @Child private AbstractReadFieldNode readStorage;

    public VectorAt(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      atPrim = AtPrimFactory.create(null, null);
      readStorage = FieldAccessorNode.createRead(2);
      lengthPrim = LengthPrimFactory.create(null);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      long index = (Long) args[1];

      SArray storage = (SArray) readStorage.read(rcvr);
      long length = lengthPrim.executeEvaluated(frame, storage);

      if (index > length) {
        return Nil.nilObject;
      }

      return atPrim.executeEvaluated(frame, storage, index);
    }
  }

  /**
   * <pre>
   * append: element = (
   * (last > storage length) ifTrue: [
   * "Need to expand capacity first"
   * | newStorage |
   * newStorage := Array new: (2 * storage length).
   * storage doIndexes: [ :i | newStorage at: i put: (storage at: i) ].
   * storage := newStorage. ].
   *
   * storage at: last put: element.
   * last := last + 1.
   * ^ self
   * )
   */
  public static final class VectorAppend extends AbstractInvokable {

    @Child private AtPutPrim     atPutPrim;
    @Child private LengthPrim    lengthPrim;
    @Child private NewPrim       newPrim;
    @Child private DoIndexesPrim doIndexesPrim;

    @Child private AbstractReadFieldNode readLast;
    @Child private AbstractReadFieldNode readStorage;

    @Child private AbstractWriteFieldNode writeLast;
    @Child private AbstractWriteFieldNode writeStorage;

    private final SMethod doIndexesBlock;

    private final FrameSlot newStorageSlot;

    public static VectorAppend create(final Source source, final long sourceCoord) {
      FrameDescriptor fd = new FrameDescriptor();
      FrameSlot newStorageSlot = fd.addFrameSlot("newStorage");
      return new VectorAppend(source, sourceCoord, fd, newStorageSlot);
    }

    private VectorAppend(final Source source, final long sourceCoord, final FrameDescriptor fd,
        final FrameSlot newStorageSlot) {
      super(fd, source, sourceCoord);
      this.newStorageSlot = newStorageSlot;

      atPutPrim = AtPutPrimFactory.create(null, null, null);

      lengthPrim = LengthPrimFactory.create(null);
      newPrim = NewPrimFactory.create(null, null);
      doIndexesPrim = DoIndexesPrimFactory.create(null, null).initialize(sourceCoord);

      readLast = FieldAccessorNode.createRead(1);
      readStorage = FieldAccessorNode.createRead(2);

      writeLast = FieldAccessorNode.createWrite(1);
      writeStorage = FieldAccessorNode.createWrite(2);

      doIndexesBlock = new SMethod(SymbolTable.symbolFor("value:"),
          new VectorAppendDoIndexesBlock(source, sourceCoord, newStorageSlot),
          new SMethod[0]);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      Object element = args[1];

      long last = readLast.readLongSafe(rcvr);

      SArray storage = (SArray) readStorage.read(rcvr);
      long length = lengthPrim.executeEvaluated(frame, storage);

      if (last > length) {
        storage = (SArray) readStorage.read(rcvr);
        length = lengthPrim.executeEvaluated(frame, storage);
        long newLength = 0;
        try {
          newLength = Math.multiplyExact(2, length);
        } catch (ArithmeticException e) {
          CompilerDirectives.transferToInterpreter();
          throw new NotYetImplementedException();
        }
        SArray newStorage =
            (SArray) newPrim.executeEvaluated(frame, Classes.arrayClass, newLength);
        frame.setObject(newStorageSlot, newStorage);

        SBlock block =
            new SBlock(doIndexesBlock, Classes.blockClasses[1], frame.materialize());
        doIndexesPrim.executeEvaluated(frame, storage, block);

        writeStorage.write(rcvr, newStorage);
      }

      atPutPrim.executeEvaluated(frame, readStorage.read(rcvr), last, element);
      try {
        writeLast.write(rcvr, Math.addExact(last, 1));
      } catch (ArithmeticException e) {
        CompilerDirectives.transferToInterpreter();
        throw new NotYetImplementedException();
      }

      return rcvr;
    }

    @Override
    public void propagateLoopCountThroughoutLexicalScope(final long count) {
      LoopNode.reportLoopCount(this, (int) count);
    }
  }

  /**
   * <pre>
   * [ :i | newStorage at: i put: (storage at: i) ].
   * </pre>
   */
  private static final class VectorAppendDoIndexesBlock extends AbstractInvokable {
    private final FrameSlot newStorageSlot;

    @Child private AtPrim    atPrim;
    @Child private AtPutPrim atPutPrim;

    @Child private AbstractReadFieldNode readStorage;

    private VectorAppendDoIndexesBlock(final Source source, final long sourceCoord,
        final FrameSlot newStorage) {
      super(new FrameDescriptor(), source, sourceCoord);
      this.newStorageSlot = newStorage;

      atPrim = AtPrimFactory.create(null, null);
      atPutPrim = AtPutPrimFactory.create(null, null, null);

      readStorage = FieldAccessorNode.createRead(2);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SBlock block = (SBlock) args[0];
      long i = (Long) args[1];
      SObject rcvr = (SObject) block.getOuterSelf();

      SArray newStorage = (SArray) FrameUtil.getObjectSafe(block.getContext(), newStorageSlot);

      Object storage = readStorage.read(rcvr);
      Object value = atPrim.executeEvaluated(frame, storage, i);
      atPutPrim.executeEvaluated(frame, newStorage, i, value);
      return value;
    }
  }
}
