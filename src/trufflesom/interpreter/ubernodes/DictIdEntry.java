package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.AbstractInvokable;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import trufflesom.primitives.basics.NewObjectPrim;
import trufflesom.primitives.basics.NewObjectPrimFactory;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SObject;


public abstract class DictIdEntry {
  /**
   * <pre>
   * match: aHash key: aKey = (
       ^ hash = aHash and: [key == aKey].
     )
   * </pre>
   */
  public static final class DictIdEntryMatchKey extends AbstractInvokable {

    @Child private AbstractReadFieldNode readHash;
    @Child private AbstractReadFieldNode readKey;

    public DictIdEntryMatchKey(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readHash = FieldAccessorNode.createRead(0);
      readKey = FieldAccessorNode.createRead(1);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      long aHash = (Long) args[1];
      Object aKey = args[2];

      long hash = readHash.readLongSafe(rcvr);
      return hash == aHash && readKey.read(rcvr) == aKey;
    }
  }

  /**
   * <pre>
    new: hash key: key value: val next: next = (
      ^ self new init: hash key: key value: val next: next.
    )
   * </pre>
   */
  public static final class DictIdEntryNewKeyValueNext extends AbstractInvokable {

    @Child private NewObjectPrim        newPrim;
    @Child private AbstractDispatchNode dispatchInit;

    public DictIdEntryNewKeyValueNext(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      newPrim = NewObjectPrimFactory.create(null);
      dispatchInit =
          new UninitializedDispatchNode(SymbolTable.symbolFor("init:key:value:next:"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      Object clazz = args[0];
      Object hash = args[1];
      Object key = args[2];
      Object val = args[3];
      Object next = args[4];

      Object newObj = newPrim.executeEvaluated(frame, clazz);
      return dispatchInit.executeDispatch(frame, new Object[] {newObj, hash, key, val, next});
    }
  }
}
