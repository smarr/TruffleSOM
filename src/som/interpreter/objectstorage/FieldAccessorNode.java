package som.interpreter.objectstorage;

import som.interpreter.MateNode;
import som.interpreter.TruffleCompiler;
import som.interpreter.TypesGen;
import som.vm.constants.Nil;
import som.vm.constants.ReflectiveOp;
import som.vmobjects.SObject;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.object.Locations.DualLocation;

public abstract class FieldAccessorNode extends Node implements MateNode {
  protected final int fieldIndex;

  public static AbstractReadFieldNode createRead(final int fieldIndex) {
    return new UninitializedReadFieldNode(fieldIndex);
  }

  public static AbstractWriteFieldNode createWrite(final int fieldIndex) {
    return new UninitializedWriteFieldNode(fieldIndex);
  }

  protected FieldAccessorNode(final int fieldIndex) {
    this.fieldIndex = fieldIndex;
  }

  public final int getFieldIndex() {
    return fieldIndex;
  }

  public abstract ReflectiveOp reflectiveOperation();

  public abstract static class AbstractReadFieldNode extends FieldAccessorNode {

    public AbstractReadFieldNode(final int fieldIndex) {
      super(fieldIndex);
    }

    public abstract Object read(SObject obj);

    public long readLong(final SObject obj) throws UnexpectedResultException {
      return TypesGen.expectLong(read(obj));
    }

    public double readDouble(final SObject obj)
        throws UnexpectedResultException {
      return TypesGen.expectDouble(read(obj));
    }

    protected final Object specializeAndRead(final SObject obj,
        final String reason, final AbstractReadFieldNode next) {
      return specialize(obj, reason, next).read(obj);
    }

    protected final AbstractReadFieldNode specialize(final SObject obj,
        final String reason, final AbstractReadFieldNode next) {
      TruffleCompiler.transferToInterpreterAndInvalidate(reason);

      final ReadSpecializedFieldNode newNode;
      final Shape layout = obj.getObjectLayout();

      final DualLocation storageLocation = (DualLocation) layout.getProperty(
          fieldIndex).getLocation();
      assert storageLocation != null;

      if (storageLocation.getType() == null) {
        /* reading uninitializedField */
        newNode = new ReadUnwrittenFieldNode(fieldIndex, layout, this);
      } else {
        Class<?> type = storageLocation.getType();
        if (type == long.class) {
          newNode = new ReadLongFieldNode(fieldIndex, layout, this);
        } else if (type == double.class) {
          newNode = new ReadDoubleFieldNode(fieldIndex, layout, this);
        } else {
          newNode = new ReadObjectFieldNode(fieldIndex, layout, this);
        }
      }
      return replace(newNode, reason);
    }

    @Override
    public void wrapIntoMateNode(){
      //replace(new MateFieldReadNode(this));
    }

    @Override
    public ReflectiveOp reflectiveOperation(){
      return ReflectiveOp.ReadLayout;
    }
  }

  public static final class UninitializedReadFieldNode extends
      AbstractReadFieldNode {

    public UninitializedReadFieldNode(final int fieldIndex) {
      super(fieldIndex);
    }

    @Override
    public Object read(final SObject obj) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      //return specializeAndRead(obj, "uninitalized node",
          //new UninitializedReadFieldNode(fieldIndex));
      return obj.getField(this.fieldIndex);
    }
  }

  public abstract static class ReadSpecializedFieldNode extends
      AbstractReadFieldNode {

    protected final Shape         layout;
    @Child
    private AbstractReadFieldNode nextInCache;

    public ReadSpecializedFieldNode(final int fieldIndex, final Shape layout,
        final AbstractReadFieldNode next) {
      super(fieldIndex);
      this.layout = layout;
      nextInCache = next;
    }

    protected final boolean hasExpectedLayout(final SObject obj) {
      return layout == obj.getObjectLayout();
    }

    protected final AbstractReadFieldNode respecializedNodeOrNext(
        final SObject obj) {
      if (this.hasExpectedLayout(obj)) {
        return specialize(obj, "update outdated read node", nextInCache);
      } else {
        return nextInCache;
      }
    }
  }

  public static final class ReadUnwrittenFieldNode extends
      ReadSpecializedFieldNode {

    public ReadUnwrittenFieldNode(final int fieldIndex, final Shape layout,
        final AbstractReadFieldNode next) {
      super(fieldIndex, layout, next);
    }

    @Override
    public Object read(final SObject obj) {
      if (hasExpectedLayout(obj)) {
        return Nil.nilObject;
      } else {
        return respecializedNodeOrNext(obj).read(obj);
      }
    }
  }

  public static final class ReadLongFieldNode extends ReadSpecializedFieldNode {

    private final DualLocation storage;

    public ReadLongFieldNode(final int fieldIndex, final Shape layout,
        final AbstractReadFieldNode next) {
      super(fieldIndex, layout, next);
      this.storage = (DualLocation) layout.getProperty(fieldIndex)
          .getLocation();
    }

    @Override
    public long readLong(final SObject obj) throws UnexpectedResultException {
      boolean assumption = hasExpectedLayout(obj);
      if (assumption) {
        return (long) storage.get(obj.getDynamicObject(), assumption);
      } else {
        return respecializedNodeOrNext(obj).readLong(obj);
      }
    }

    @Override
    public Object read(final SObject obj) {
      try {
        return readLong(obj);
      } catch (UnexpectedResultException e) {
        return e.getResult();
      }
    }
  }

  public static final class ReadDoubleFieldNode extends
      ReadSpecializedFieldNode {

    private final DualLocation storage;

    public ReadDoubleFieldNode(final int fieldIndex, final Shape layout,
        final AbstractReadFieldNode next) {
      super(fieldIndex, layout, next);
      this.storage = (DualLocation) layout.getProperty(fieldIndex)
          .getLocation();
    }

    @Override
    public double readDouble(final SObject obj)
        throws UnexpectedResultException {
      boolean assumption = hasExpectedLayout(obj);
      if (assumption) {
        return (double) storage.get(obj.getDynamicObject(), assumption);
      } else {
        return respecializedNodeOrNext(obj).readDouble(obj);
      }
    }

    @Override
    public Object read(final SObject obj) {
      try {
        return readDouble(obj);
      } catch (UnexpectedResultException e) {
        return e.getResult();
      }
    }
  }

  public static final class ReadObjectFieldNode extends
      ReadSpecializedFieldNode {

    private final Location storage;

    public ReadObjectFieldNode(final int fieldIndex, final Shape layout,
        final AbstractReadFieldNode next) {
      super(fieldIndex, layout, next);
      this.storage = layout.getProperty(fieldIndex).getLocation();
    }

    @Override
    public Object read(final SObject obj) {
      boolean assumption = hasExpectedLayout(obj);
      if (assumption) {
        return storage.get(obj.getDynamicObject(), assumption);
      } else {
        return respecializedNodeOrNext(obj).read(obj);
      }
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

    protected final void writeAndRespecialize(final SObject obj,
        final Object value, final String reason,
        final AbstractWriteFieldNode next) {
      TruffleCompiler.transferToInterpreterAndInvalidate(reason);

      obj.setField(fieldIndex, value);

      final WriteSpecializedFieldNode newNode;
      final Shape layout = obj.getObjectLayout();

      if (value instanceof Long) {
        newNode = new WriteLongFieldNode(fieldIndex, layout, this);
      } else if (value instanceof Double) {
        newNode = new WriteDoubleFieldNode(fieldIndex, layout, this);
      } else {
        newNode = new WriteObjectFieldNode(fieldIndex, layout, this);
      }
      replace(newNode, reason);
    }

    @Override
    public void wrapIntoMateNode(){
      //replace(new MateFieldWriteNode(this));
    }

    @Override
    public ReflectiveOp reflectiveOperation(){
      return ReflectiveOp.WriteLayout;
    }
  }

  public static final class UninitializedWriteFieldNode extends
      AbstractWriteFieldNode {

    public UninitializedWriteFieldNode(final int fieldIndex) {
      super(fieldIndex);
    }

    @Override
    public Object write(final SObject obj, final Object value) {
      if (value != Nil.nilObject) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        writeAndRespecialize(obj, value, "initialize write field node",
            new UninitializedWriteFieldNode(fieldIndex));
      }
      return value;
    }
  }

  private abstract static class WriteSpecializedFieldNode extends
      AbstractWriteFieldNode {

    protected final Shape            layout;
    @Child
    protected AbstractWriteFieldNode nextInCache;

    public WriteSpecializedFieldNode(final int fieldIndex, final Shape layout,
        final AbstractWriteFieldNode next) {
      super(fieldIndex);
      this.layout = layout;
      nextInCache = next;
    }

    protected final boolean hasExpectedLayout(final SObject obj) {
      return layout.check(obj.getDynamicObject());
    }
  }

  public static final class WriteLongFieldNode extends
      WriteSpecializedFieldNode {

    //private final DualLocation storage;

    public WriteLongFieldNode(final int fieldIndex, final Shape layout,
        final AbstractWriteFieldNode next) {
      super(fieldIndex, layout, next);
      //this.storage = (DualLocation) layout.getProperty(fieldIndex).getLocation();
    }

    @Override
    public long write(final SObject obj, final long value) {
      obj.setField(fieldIndex, value);
      /*if (hasExpectedLayout(obj)) {
        try {
          storage.set(obj.getDynamicObject(), value);
        } catch (IncompatibleLocationException | FinalLocationException e) {
          e.printStackTrace();
        }
      } else {
        if (obj.getNumberOfFields() == this.layout.getPropertyCount()) {
          writeAndRespecialize(obj, value,"",nextInCache);
        } else {
          nextInCache.write(obj, value);
        }
      }*/
      return value;
    }

    @Override
    public Object write(final SObject obj, final Object value) {
      if (value instanceof Long) {
        write(obj, (long) value);
      } else {
        if (obj.getNumberOfFields() == this.layout.getPropertyCount()) {
          writeAndRespecialize(obj, value, "update outdated read node",
              nextInCache);
        } else {
          nextInCache.write(obj, value);
        }
      }
      return value;
    }
  }

  public static final class WriteDoubleFieldNode extends
      WriteSpecializedFieldNode {

    //private final DualLocation storage;

    public WriteDoubleFieldNode(final int fieldIndex, final Shape layout,
        final AbstractWriteFieldNode next) {
      super(fieldIndex, layout, next);
      //this.storage = (DualLocation) layout.getProperty(fieldIndex).getLocation();
    }

    @Override
    public double write(final SObject obj, final double value) {
      obj.setField(fieldIndex, value);
      /*if (hasExpectedLayout(obj)) {
        try {
          storage.set(obj.getDynamicObject(), value);
        } catch (FinalLocationException | IncompatibleLocationException e) {
          e.printStackTrace();
          //writeAndRespecialize(obj, value, "update outdated read node", nextInCache);
        }
      } else {
        if (obj.getNumberOfFields() == this.layout.getPropertyCount()) {
          writeAndRespecialize(obj, value, "", nextInCache);
        } else {
          nextInCache.write(obj, value);
        }
      }*/
      return value;
    }

    @Override
    public Object write(final SObject obj, final Object value) {
      if (value instanceof Double) {
        write(obj, (double) value);
      } else {
        if (obj.getNumberOfFields() == this.layout.getPropertyCount()) {
          writeAndRespecialize(obj, value, "update outdated read node",
              nextInCache);
        } else {
          nextInCache.write(obj, value);
        }
      }
      return value;
    }
  }

  public static final class WriteObjectFieldNode extends
      WriteSpecializedFieldNode {

    //private final DualLocation storage;

    public WriteObjectFieldNode(final int fieldIndex, final Shape layout,
        final AbstractWriteFieldNode next) {
      super(fieldIndex, layout, next);
      //this.storage = (DualLocation) layout.getProperty(fieldIndex).getLocation();
    }

    @Override
    public Object write(final SObject obj, final Object value) {
      obj.setField(fieldIndex, value);
      /*if (hasExpectedLayout(obj)) {
        try {
          storage.set(obj.getDynamicObject(), value);
        } catch (IncompatibleLocationException | FinalLocationException e) {
          //This case can only happen if there was an explicit write with nil to previously unwritten fields
          writeAndRespecialize(obj, value, "update outdated read node", nextInCache);
        }
      } else {
        if (obj.getNumberOfFields() == this.layout.getPropertyCount()) {
          writeAndRespecialize(obj, value, "", nextInCache);
        } else {
          nextInCache.write(obj, value);
        }
      }*/
      return value;
    }
  }
}
