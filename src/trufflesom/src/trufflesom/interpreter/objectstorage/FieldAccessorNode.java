package trufflesom.interpreter.objectstorage;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import trufflesom.interpreter.TypesGen;
import trufflesom.interpreter.nodes.SOMNode;
import trufflesom.interpreter.objectstorage.StorageLocation.AbstractObjectStorageLocation;
import trufflesom.interpreter.objectstorage.StorageLocation.DoubleStorageLocation;
import trufflesom.interpreter.objectstorage.StorageLocation.LongStorageLocation;
import trufflesom.vm.VmSettings;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SObject;


public abstract class FieldAccessorNode extends Node {
  public static final int INLINE_CACHE_SIZE = 6;

  protected final int fieldIndex;

  @InliningCutoff
  public static AbstractReadFieldNode createRead(final int fieldIndex) {
    return new UninitializedReadFieldNode(fieldIndex, 0);
  }

  @InliningCutoff
  public static AbstractWriteFieldNode createWrite(final int fieldIndex) {
    return new UninitializedWriteFieldNode(fieldIndex, 0);
  }

  @InliningCutoff
  public static IncrementLongFieldNode createIncrement(final int fieldIndex,
      final SObject obj) {
    final ObjectLayout layout = obj.getObjectLayout();
    return new IncrementLongFieldNode(fieldIndex, layout);
  }

  private FieldAccessorNode(final int fieldIndex) {
    this.fieldIndex = fieldIndex;
  }

  public final int getFieldIndex() {
    return fieldIndex;
  }

  public void notifyAsInserted() {
    if (VmSettings.UseInstrumentation) {
      notifyInserted(this);
    }
  }

  public abstract static class AbstractReadFieldNode extends FieldAccessorNode {
    public AbstractReadFieldNode(final int fieldIndex) {
      super(fieldIndex);
    }

    public abstract Object read(SObject obj);

    public long readLong(final SObject obj) throws UnexpectedResultException {
      return TypesGen.expectLong(read(obj));
    }

    public double readDouble(final SObject obj) throws UnexpectedResultException {
      return TypesGen.expectDouble(read(obj));
    }

    @InliningCutoff
    protected final AbstractReadFieldNode specialize(final SObject obj, int chainLength,
        final String reason, final AbstractReadFieldNode next) {
      CompilerDirectives.transferToInterpreterAndInvalidate();

      if (chainLength >= INLINE_CACHE_SIZE) {
        GenericReadFieldNode genericReplacement = new GenericReadFieldNode(fieldIndex);
        replace(genericReplacement, "megamorphic read node");
        return genericReplacement;
      }

      obj.updateLayoutToMatchClass();

      final ObjectLayout layout = obj.getObjectLayout();
      final StorageLocation location = layout.getStorageLocation(fieldIndex);

      AbstractReadFieldNode newNode = location.getReadNode(fieldIndex, layout, next);
      return replace(newNode, reason);
    }
  }

  private static final class GenericReadFieldNode extends AbstractReadFieldNode {
    GenericReadFieldNode(final int fieldIndex) {
      super(fieldIndex);
    }

    @Override
    public Object read(final SObject obj) {
      if (obj.hasOutdatedLayout()) {
        // this case is rare, we do not invalidate here, but also really don't want this to be
        // in the compilation
        CompilerDirectives.transferToInterpreter();
        obj.updateLayoutToMatchClass();
      }
      StorageLocation location = obj.getLocation(fieldIndex);
      return location.read(obj);
    }
  }

  private static final class UninitializedReadFieldNode extends AbstractReadFieldNode {

    private final int chainLength;

    UninitializedReadFieldNode(final int fieldIndex, int chainLength) {
      super(fieldIndex);
      this.chainLength = chainLength;
    }

    @Override
    @InliningCutoff
    public Object read(final SObject obj) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      return specialize(obj, chainLength, "uninitialized node",
          new UninitializedReadFieldNode(fieldIndex, chainLength + 1)).read(obj);
    }
  }

  public abstract static class ReadSpecializedFieldNode extends AbstractReadFieldNode {
    protected final ObjectLayout           layout;
    @Child protected AbstractReadFieldNode nextInCache;

    public ReadSpecializedFieldNode(final int fieldIndex,
        final ObjectLayout layout, final AbstractReadFieldNode next) {
      super(fieldIndex);
      this.layout = layout;
      nextInCache = next;
    }

    protected final boolean hasExpectedLayout(final SObject obj)
        throws InvalidAssumptionException {
      layout.checkIsLatest();
      return layout == obj.getObjectLayout();
    }
  }

  public static final class ReadUnwrittenFieldNode extends ReadSpecializedFieldNode {
    public ReadUnwrittenFieldNode(final int fieldIndex,
        final ObjectLayout layout, final AbstractReadFieldNode next) {
      super(fieldIndex, layout, next);
    }

    @Override
    public Object read(final SObject obj) {
      try {
        if (hasExpectedLayout(obj)) {
          return Nil.nilObject;
        } else {
          return nextInCache.read(obj);
        }
      } catch (InvalidAssumptionException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        return dropAndReadNext(obj);
      }
    }

    @InliningCutoff
    private Object dropAndReadNext(final SObject obj) {
      return replace(SOMNode.unwrapIfNeeded(nextInCache)).read(obj);
    }
  }

  public static final class ReadLongFieldNode extends ReadSpecializedFieldNode {
    private final LongStorageLocation                    storage;
    private @CompilerDirectives.CompilationFinal boolean wasSeenUnset;

    public ReadLongFieldNode(final int fieldIndex, final ObjectLayout layout,
        final AbstractReadFieldNode next) {
      super(fieldIndex, layout, next);
      this.storage = (LongStorageLocation) layout.getStorageLocation(fieldIndex);
      wasSeenUnset = false;
    }

    @Override
    public long readLong(final SObject obj) throws UnexpectedResultException {
      try {
        if (hasExpectedLayout(obj)) {
          return storage.readLong(obj);
        } else {
          return nextInCache.readLong(obj);
        }
      } catch (InvalidAssumptionException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        return dropAndReadLongNext(obj);
      }
    }

    @InliningCutoff
    private long dropAndReadLongNext(final SObject obj) throws UnexpectedResultException {
      return replace(SOMNode.unwrapIfNeeded(nextInCache)).readLong(obj);
    }

    @InliningCutoff
    private Object dropAndReadNext(final SObject obj) {
      return replace(SOMNode.unwrapIfNeeded(nextInCache)).read(obj);
    }

    @Override
    public Object read(final SObject obj) {
      try {
        if (hasExpectedLayout(obj)) {
          if (!storage.isSet(obj)) {
            if (!wasSeenUnset) {
              CompilerDirectives.transferToInterpreterAndInvalidate();
              wasSeenUnset = true;
            }
            return Nil.nilObject;
          }

          return storage.readLongSet(obj);
        } else {
          return nextInCache.read(obj);
        }
      } catch (InvalidAssumptionException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        return dropAndReadNext(obj);
      }
    }
  }

  public static final class ReadDoubleFieldNode extends ReadSpecializedFieldNode {
    private final DoubleStorageLocation                  storage;
    private @CompilerDirectives.CompilationFinal boolean wasSeenUnset;

    public ReadDoubleFieldNode(final int fieldIndex, final ObjectLayout layout,
        final AbstractReadFieldNode next) {
      super(fieldIndex, layout, next);
      this.storage = (DoubleStorageLocation) layout.getStorageLocation(fieldIndex);
      wasSeenUnset = false;
    }

    @Override
    public double readDouble(final SObject obj) throws UnexpectedResultException {
      try {
        if (hasExpectedLayout(obj)) {
          return storage.readDouble(obj);
        } else {
          return nextInCache.readDouble(obj);
        }
      } catch (InvalidAssumptionException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        return dropAndReadDoubleNext(obj);
      }
    }

    @InliningCutoff
    private double dropAndReadDoubleNext(final SObject obj) throws UnexpectedResultException {
      return replace(SOMNode.unwrapIfNeeded(nextInCache)).readDouble(obj);
    }

    @InliningCutoff
    private Object dropAndReadNext(final SObject obj) {
      return replace(SOMNode.unwrapIfNeeded(nextInCache)).read(obj);
    }

    @Override
    public Object read(final SObject obj) {
      try {
        if (hasExpectedLayout(obj)) {
          if (!storage.isSet(obj)) {
            if (!wasSeenUnset) {
              CompilerDirectives.transferToInterpreterAndInvalidate();
              wasSeenUnset = true;
            }
            return Nil.nilObject;
          }

          return storage.readDoubleSet(obj);
        } else {
          return nextInCache.read(obj);
        }
      } catch (InvalidAssumptionException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        return dropAndReadNext(obj);
      }
    }
  }

  public static final class ReadObjectFieldNode extends ReadSpecializedFieldNode {
    private final AbstractObjectStorageLocation storage;

    public ReadObjectFieldNode(final int fieldIndex, final ObjectLayout layout,
        final AbstractReadFieldNode next) {
      super(fieldIndex, layout, next);
      this.storage = (AbstractObjectStorageLocation) layout.getStorageLocation(fieldIndex);
    }

    @Override
    public Object read(final SObject obj) {
      try {
        if (hasExpectedLayout(obj)) {
          return storage.read(obj);
        } else {
          CompilerAsserts.partialEvaluationConstant(nextInCache);
          return nextInCache.read(obj);
        }
      } catch (InvalidAssumptionException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        return dropAndReadNext(obj);
      }
    }

    @InliningCutoff
    private Object dropAndReadNext(final SObject obj) {
      return replace(SOMNode.unwrapIfNeeded(nextInCache)).read(obj);
    }
  }

  public abstract static class AbstractWriteFieldNode extends FieldAccessorNode {
    public AbstractWriteFieldNode(final int fieldIndex) {
      super(fieldIndex);
    }

    public abstract Object write(SObject obj, Object value);

    public long write(final SObject obj, final long value) {
      write(obj, (Object) value);
      return value;
    }

    public double write(final SObject obj, final double value) {
      write(obj, (Object) value);
      return value;
    }

    @InliningCutoff
    protected final void writeToOutdated(final SObject obj, final Object value) {
      CompilerDirectives.transferToInterpreter();
      assert !obj.getObjectLayout().isValid();
      obj.setField(fieldIndex, value);
    }

    @InliningCutoff
    protected final void writeUnexpectedTypeAndRespecialize(final SObject obj,
        final Object value, final AbstractWriteFieldNode next) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      obj.setField(fieldIndex, value);

      final ObjectLayout layout = obj.getObjectLayout();
      final StorageLocation location = layout.getStorageLocation(fieldIndex);
      AbstractWriteFieldNode newNode = location.getWriteNode(fieldIndex, layout, next);
      replace(newNode, "update outdated read node");
    }
  }

  private static final class UninitializedWriteFieldNode extends AbstractWriteFieldNode {
    private final int chainLength;

    UninitializedWriteFieldNode(final int fieldIndex, int chainLength) {
      super(fieldIndex);
      this.chainLength = chainLength;
    }

    @Override
    @InliningCutoff
    public Object write(final SObject obj, final Object value) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      obj.setField(fieldIndex, value);

      if (chainLength >= INLINE_CACHE_SIZE) {
        GenericWriteFieldNode generic = new GenericWriteFieldNode(fieldIndex);
        replace(generic, "megamorphic write node");
        return value;
      }

      final ObjectLayout layout = obj.getObjectLayout();
      final StorageLocation location = layout.getStorageLocation(fieldIndex);
      AbstractWriteFieldNode newNode = location.getWriteNode(fieldIndex, layout,
          new UninitializedWriteFieldNode(fieldIndex, chainLength + 1));
      replace(newNode, "initialize write field node");

      return value;
    }
  }

  private static final class GenericWriteFieldNode extends AbstractWriteFieldNode {
    GenericWriteFieldNode(final int fieldIndex) {
      super(fieldIndex);
    }

    @Override
    public Object write(final SObject obj, final Object value) {
      StorageLocation location = obj.getLocation(fieldIndex);
      location.write(obj, value);
      return value;
    }
  }

  private abstract static class WriteSpecializedFieldNode extends AbstractWriteFieldNode {

    protected final ObjectLayout            layout;
    @Child protected AbstractWriteFieldNode nextInCache;

    WriteSpecializedFieldNode(final int fieldIndex,
        final ObjectLayout layout, final AbstractWriteFieldNode next) {
      super(fieldIndex);
      this.layout = layout;
      nextInCache = next;
    }

    protected final boolean hasExpectedLayout(final SObject obj)
        throws InvalidAssumptionException {
      layout.checkIsLatest();
      return layout == obj.getObjectLayout();
    }
  }

  public static final class IncrementLongFieldNode extends FieldAccessorNode {
    private final ObjectLayout        layout;
    private final LongStorageLocation storage;

    @Child private IncrementLongFieldNode nextInCache;

    public IncrementLongFieldNode(final int fieldIndex, final ObjectLayout layout) {
      super(fieldIndex);
      this.layout = layout;
      this.storage = (LongStorageLocation) layout.getStorageLocation(fieldIndex);
    }

    private boolean hasExpectedLayout(final SObject obj)
        throws InvalidAssumptionException {
      layout.checkIsLatest();
      return layout == obj.getObjectLayout();
    }

    public long increment(final SObject obj) {
      try {
        if (hasExpectedLayout(obj)) {
          return storage.increment(obj);
        } else {
          ensureNext(obj);
          return nextInCache.increment(obj);
        }
      } catch (InvalidAssumptionException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        ensureNext(obj);
        return dropAndIncrementNext(obj);
      }
    }

    @InliningCutoff
    private long dropAndIncrementNext(final SObject obj) {
      return replace(SOMNode.unwrapIfNeeded(nextInCache)).increment(obj);
    }

    @InliningCutoff
    private void ensureNext(final SObject obj) {
      if (nextInCache == null) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        nextInCache = new IncrementLongFieldNode(fieldIndex, obj.getObjectLayout());
      }
    }
  }

  public static final class WriteLongFieldNode extends WriteSpecializedFieldNode {
    private final LongStorageLocation storage;

    public WriteLongFieldNode(final int fieldIndex, final ObjectLayout layout,
        final AbstractWriteFieldNode next) {
      super(fieldIndex, layout, next);
      this.storage = (LongStorageLocation) layout.getStorageLocation(fieldIndex);
    }

    @Override
    public long write(final SObject obj, final long value) {
      try {
        if (hasExpectedLayout(obj)) {
          storage.writeLong(obj, value);
        } else {
          if (layout.layoutForSameClass(obj.getObjectLayout())) {
            writeToOutdated(obj, value);
          } else {
            nextInCache.write(obj, value);
          }
        }
      } catch (InvalidAssumptionException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        dropAndWriteNext(obj, value);
      }
      return value;
    }

    @InliningCutoff
    private void dropAndWriteNext(final SObject obj, final long value) {
      replace(SOMNode.unwrapIfNeeded(nextInCache)).write(obj, value);
    }

    @Override
    public Object write(final SObject obj, final Object value) {
      if (value instanceof Long) {
        write(obj, (long) value);
      } else if (layout.layoutForSameClass(obj.getObjectLayout())) {
        writeUnexpectedTypeAndRespecialize(obj, value, nextInCache);
      } else {
        nextInCache.write(obj, value);
      }
      return value;
    }
  }

  public static final class WriteDoubleFieldNode extends WriteSpecializedFieldNode {
    private final DoubleStorageLocation storage;

    public WriteDoubleFieldNode(final int fieldIndex, final ObjectLayout layout,
        final AbstractWriteFieldNode next) {
      super(fieldIndex, layout, next);
      this.storage = (DoubleStorageLocation) layout.getStorageLocation(fieldIndex);
    }

    @Override
    public double write(final SObject obj, final double value) {
      try {
        if (hasExpectedLayout(obj)) {
          storage.writeDouble(obj, value);
        } else {
          if (layout.layoutForSameClass(obj.getObjectLayout())) {
            writeToOutdated(obj, value);
          } else {
            nextInCache.write(obj, value);
          }
        }
      } catch (InvalidAssumptionException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        dropAndWriteNext(obj, value);
      }
      return value;
    }

    @InliningCutoff
    private void dropAndWriteNext(final SObject obj, final double value) {
      replace(SOMNode.unwrapIfNeeded(nextInCache)).write(obj, value);
    }

    @Override
    public Object write(final SObject obj, final Object value) {
      if (value instanceof Double) {
        write(obj, (double) value);
      } else if (layout.layoutForSameClass(obj.getObjectLayout())) {
        writeUnexpectedTypeAndRespecialize(obj, value, nextInCache);
      } else {
        nextInCache.write(obj, value);
      }
      return value;
    }
  }

  public static final class WriteObjectFieldNode extends WriteSpecializedFieldNode {
    private final AbstractObjectStorageLocation storage;

    public WriteObjectFieldNode(final int fieldIndex, final ObjectLayout layout,
        final AbstractWriteFieldNode next) {
      super(fieldIndex, layout, next);
      this.storage = (AbstractObjectStorageLocation) layout.getStorageLocation(fieldIndex);
    }

    @Override
    public Object write(final SObject obj, final Object value) {
      try {
        if (hasExpectedLayout(obj)) {
          storage.write(obj, value);
        } else if (layout.layoutForSameClass(obj.getObjectLayout())) {
          writeToOutdated(obj, value);
        } else {
          nextInCache.write(obj, value);
        }
      } catch (InvalidAssumptionException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        dropAndWriteNext(obj, value);
      }
      return value;
    }

    @InliningCutoff
    private void dropAndWriteNext(final SObject obj, final Object value) {
      replace(SOMNode.unwrapIfNeeded(nextInCache)).write(obj, value);
    }
  }
}
