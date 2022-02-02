package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.AbstractInvokable;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;
import trufflesom.primitives.arrays.AtPrim;
import trufflesom.primitives.arrays.AtPrimFactory;
import trufflesom.primitives.arrays.NewPrim;
import trufflesom.primitives.arrays.NewPrimFactory;
import trufflesom.primitives.basics.LengthPrim;
import trufflesom.primitives.basics.LengthPrimFactory;
import trufflesom.primitives.basics.NewObjectPrim;
import trufflesom.primitives.basics.NewObjectPrimFactory;
import trufflesom.vm.Classes;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SClass;
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
}
