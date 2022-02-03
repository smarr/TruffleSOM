package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.CompilerDirectives;
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
import trufflesom.primitives.arrays.AtPutPrim;
import trufflesom.primitives.arrays.AtPutPrimFactory;
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

  /**
   * <pre>
   *  at: aKey put: aVal = (
    | hash i current |
    hash := self hash: aKey.
    i    := self bucketIdx: hash.
    current := buckets at: i.
  
    current isNil
      ifTrue: [
        buckets at: i put: (self newEntry: aKey value: aVal hash: hash).
        size_ := size_ + 1 ]
      ifFalse: [
        self insertBucketEntry: aKey value: aVal hash: hash head: current ].

    size_ > buckets length ifTrue: [ self resize ]
  )
   * </pre>
   *
   */
  public static final class SomDictAtPut extends AbstractInvokable {

    @Child private AbstractReadFieldNode  readBuckets;
    @Child private AbstractReadFieldNode  readSize;
    @Child private AbstractWriteFieldNode writeSize;

    @Child private AtPrim     atPrim;
    @Child private AtPutPrim  atPutPrim;
    @Child private LengthPrim lengthPrim;

    @Child private AbstractDispatchNode dispatchHash;
    @Child private AbstractDispatchNode dispatchBucketIdx;
    @Child private AbstractDispatchNode dispatchNewEntry;
    @Child private AbstractDispatchNode dispatchInsertBucketEntry;
    @Child private AbstractDispatchNode dispatchResize;

    public SomDictAtPut(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readBuckets = FieldAccessorNode.createRead(0);
      readSize = FieldAccessorNode.createRead(1);
      writeSize = FieldAccessorNode.createWrite(1);

      atPrim = AtPrimFactory.create(null, null);
      atPutPrim = AtPutPrimFactory.create(null, null, null);
      lengthPrim = LengthPrimFactory.create(null);

      dispatchHash =
          new UninitializedDispatchNode(SymbolTable.symbolFor("hash:"));
      dispatchBucketIdx =
          new UninitializedDispatchNode(SymbolTable.symbolFor("bucketIdx:"));
      dispatchNewEntry =
          new UninitializedDispatchNode(SymbolTable.symbolFor("newEntry:value:hash:"));
      dispatchInsertBucketEntry =
          new UninitializedDispatchNode(
              SymbolTable.symbolFor("insertBucketEntry:value:hash:head:"));
      dispatchResize =
          new UninitializedDispatchNode(SymbolTable.symbolFor("resize"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      Object aKey = args[1];
      Object aVal = args[2];

      Object hash = dispatchHash.executeDispatch(frame, new Object[] {rcvr, aKey});
      Object i = dispatchBucketIdx.executeDispatch(frame, new Object[] {rcvr, hash});

      SArray buckets = (SArray) readBuckets.read(rcvr);
      Object current = atPrim.executeEvaluated(frame, buckets, i);

      if (current == Nil.nilObject) {
        Object newEntry =
            dispatchNewEntry.executeDispatch(frame, new Object[] {rcvr, aKey, aVal, hash});
        atPutPrim.executeEvaluated(frame, buckets, i, newEntry);

        long size = readSize.readLongSafe(rcvr);
        try {
          size = Math.addExact(size, 1);
        } catch (ArithmeticException e) {
          CompilerDirectives.transferToInterpreter();
          throw new UnsupportedOperationException();
        }

        writeSize.write(rcvr, size);
      } else {
        dispatchInsertBucketEntry.executeDispatch(frame,
            new Object[] {rcvr, aKey, aVal, hash, current});
      }

      if (readSize.readLongSafe(rcvr) > lengthPrim.executeEvaluated(frame, buckets)) {
        dispatchResize.executeDispatch(frame, new Object[] {rcvr});
      }
      return rcvr;
    }
  }

  /**
   * <pre>
   *
  insertBucketEntry: key value: value hash: hash head: head = (
    | current |
    current := head.
  
    [true] whileTrue: [
      (current match: hash key: key) ifTrue: [
        current value: value.
        ^ self ].
      current next isNil ifTrue: [
        size_ := size_ + 1.
        current next: (self newEntry: key value: value hash: hash).
        ^ self ].
      current := current next ]
  )
   * </pre>
   *
   */
  public static final class SomDictInsertBucketEntry extends AbstractInvokable {

    @Child private AbstractDispatchNode dispatchMatchKey;
    @Child private AbstractDispatchNode dispatchValue;
    @Child private AbstractDispatchNode dispatchNext;
    @Child private AbstractDispatchNode dispatchNext_;
    @Child private AbstractDispatchNode dispatchNewEntry;

    @Child private AbstractReadFieldNode  readSize;
    @Child private AbstractWriteFieldNode writeSize;

    public SomDictInsertBucketEntry(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      dispatchMatchKey =
          new UninitializedDispatchNode(SymbolTable.symbolFor("match:key:"));
      dispatchValue =
          new UninitializedDispatchNode(SymbolTable.symbolFor("value:"));
      dispatchNext =
          new UninitializedDispatchNode(SymbolTable.symbolFor("next"));
      dispatchNext_ =
          new UninitializedDispatchNode(SymbolTable.symbolFor("next:"));
      dispatchNewEntry =
          new UninitializedDispatchNode(SymbolTable.symbolFor("newEntry:value:hash:"));

      readSize = FieldAccessorNode.createRead(1);
      writeSize = FieldAccessorNode.createWrite(1);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      Object key = args[1];
      Object value = args[2];
      Object hash = args[3];
      Object head = args[4];

      // current := head.
      Object current = head;

      // [true] whileTrue: [
      while (true) {
        // (current match: hash key: key) ifTrue: [
        boolean matches = (Boolean) dispatchMatchKey.executeDispatch(frame,
            new Object[] {current, hash, key});
        if (matches) {
          // current value: value.
          dispatchValue.executeDispatch(frame, new Object[] {current, value});

          // ^ self
          return rcvr;
        }

        // current next isNil ifTrue: [
        Object currentNext = dispatchNext.executeDispatch(frame, new Object[] {current});
        if (currentNext == Nil.nilObject) {
          // size_ := size_ + 1.
          long size = readSize.readLongSafe(rcvr);
          try {
            size = Math.addExact(size, 1);
          } catch (ArithmeticException e) {
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedOperationException();
          }

          writeSize.write(rcvr, size);

          // current next: (self newEntry: key value: value hash: hash).
          Object newEntry =
              dispatchNewEntry.executeDispatch(frame, new Object[] {rcvr, key, value, hash});
          dispatchNext_.executeDispatch(frame, new Object[] {current, newEntry});

          // ^ self
          return rcvr;
        }

        // current := current next
        current = dispatchNext.executeDispatch(frame, new Object[] {current});
      }
    }
  }
}
