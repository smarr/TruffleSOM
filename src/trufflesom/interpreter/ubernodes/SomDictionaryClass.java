package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.AbstractInvokable;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import trufflesom.primitives.arrays.AtPrim;
import trufflesom.primitives.arrays.AtPrimFactory;
import trufflesom.primitives.basics.LengthPrim;
import trufflesom.primitives.basics.LengthPrimFactory;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SObject;


public abstract class SomDictionaryClass {
  /**
   * <pre>
   *   hash: key = (
    | hash |
    key isNil ifTrue: [ ^ 0 ].
    hash := key customHash.
    ^ hash bitXor: (hash >>> 16)
  )
   * </pre>
   */
  public static final class SomDictHash extends AbstractInvokable {

    @Child private AbstractDispatchNode dispatchCustomHash;

    public SomDictHash(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      dispatchCustomHash =
          new UninitializedDispatchNode(SymbolTable.symbolFor("customHash"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object key = frame.getArguments()[1];

      if (key == Nil.nilObject) {
        return 0L;
      }

      long hash = (Long) dispatchCustomHash.executeDispatch(frame, new Object[] {key});
      return hash ^ (hash >>> 16);
    }
  }

  /**
   * <pre>
  bucketIdx: hash = (
    ^ 1 + ((buckets length - 1) & hash).
  )
   * </pre>
   */
  public static final class SomDictBucketIdx extends AbstractInvokable {

    @Child private LengthPrim            lengthPrim;
    @Child private AbstractReadFieldNode readBuckets;

    public SomDictBucketIdx(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      lengthPrim = LengthPrimFactory.create(null);
      readBuckets = FieldAccessorNode.createRead(0);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      long hash = (Long) args[1];

      SArray buckets = (SArray) readBuckets.read(rcvr);
      long length = lengthPrim.executeEvaluated(frame, buckets);
      return 1L + (length - 1L & hash);
    }
  }

  /**
   * <pre>
  bucket: hash = (
    ^ buckets at: (self bucketIdx: hash).
  )
   * </pre>
   */
  public static final class SomDictBucket extends AbstractInvokable {

    @Child private AbstractReadFieldNode readBuckets;
    @Child private AbstractDispatchNode  dispatchBucketIdx;
    @Child private AtPrim                atPrim;

    public SomDictBucket(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readBuckets = FieldAccessorNode.createRead(0);
      dispatchBucketIdx =
          new UninitializedDispatchNode(SymbolTable.symbolFor("bucketIdx:"));
      atPrim = AtPrimFactory.create(null, null);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      Object hash = args[1];

      Object idx = dispatchBucketIdx.executeDispatch(frame, new Object[] {rcvr, hash});
      Object buckets = readBuckets.read(rcvr);
      return atPrim.executeEvaluated(frame, buckets, idx);
    }
  }

  /**
   * <pre>
   * at: aKey = (
        | hash e |
        hash := self hash: aKey.
        e    := self bucket: hash.

        [ e notNil ] whileTrue: [
          (e match: hash key: aKey)
            ifTrue: [ ^ e value ].
          e := e next ].
        ^ nil
      )
   * </pre>
   */
  public static final class SomDictAt extends AbstractInvokable {

    @Child private AbstractDispatchNode dispatchHash;
    @Child private AbstractDispatchNode dispatchBucket;
    @Child private AbstractDispatchNode dispatchMatchKey;
    @Child private AbstractDispatchNode dispatchValue;
    @Child private AbstractDispatchNode dispatchNext;

    public SomDictAt(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      dispatchHash =
          new UninitializedDispatchNode(SymbolTable.symbolFor("hash:"));
      dispatchBucket =
          new UninitializedDispatchNode(SymbolTable.symbolFor("bucket:"));
      dispatchMatchKey =
          new UninitializedDispatchNode(SymbolTable.symbolFor("match:key:"));
      dispatchValue =
          new UninitializedDispatchNode(SymbolTable.symbolFor("value"));
      dispatchNext =
          new UninitializedDispatchNode(SymbolTable.symbolFor("next"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      Object aKey = args[1];

      Object hash = dispatchHash.executeDispatch(frame, new Object[] {rcvr, aKey});
      Object e = dispatchBucket.executeDispatch(frame, new Object[] {rcvr, hash});

      while (e != Nil.nilObject) {
        boolean matched =
            (Boolean) dispatchMatchKey.executeDispatch(frame, new Object[] {e, hash, aKey});
        if (matched) {
          return dispatchValue.executeDispatch(frame, new Object[] {e});
        }
        e = dispatchNext.executeDispatch(frame, new Object[] {e});
      }

      return Nil.nilObject;
    }
  }
}
