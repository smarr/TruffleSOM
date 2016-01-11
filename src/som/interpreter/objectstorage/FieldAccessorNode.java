package som.interpreter.objectstorage;

import som.interpreter.objectstorage.FieldAccessorNodeFactory.ReadFieldNodeGen;
import som.interpreter.objectstorage.FieldAccessorNodeFactory.WriteFieldNodeGen;
import som.vm.constants.Nil;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.FinalLocationException;
import com.oracle.truffle.api.object.IncompatibleLocationException;
import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;


public abstract class FieldAccessorNode extends Node {
  protected final int LIMIT = 10;

  protected final int fieldIndex;

  public static ReadFieldNode createRead(final int fieldIndex) {
    return ReadFieldNodeGen.create(fieldIndex);
  }

  public static WriteFieldNode createWrite(final int fieldIndex) {
    return WriteFieldNodeGen.create(fieldIndex);
  }

  private FieldAccessorNode(final int fieldIndex) {
    this.fieldIndex = fieldIndex;
  }

  public final int getFieldIndex() {
    return fieldIndex;
  }

  protected Location getLocation(final DynamicObject obj) {
    Property property = obj.getShape().getProperty(fieldIndex);
    if (property != null) {
      return property.getLocation();
    } else {
      return null;
    }
  }

  protected Location getLocation(final DynamicObject obj, final Object value) {
    Location location = getLocation(obj);
    if (location != null && location.canSet(obj, value)) {
      return location;
    } else {
      return null;
    }
  }

  protected static final Assumption createAssumption() {
    return Truffle.getRuntime().createAssumption();
  }

  public abstract static class ReadFieldNode extends FieldAccessorNode {
    public ReadFieldNode(final int fieldIndex) {
      super(fieldIndex);
    }

    public abstract Object executeRead(DynamicObject obj);

    @Specialization(guards = {"self.getShape() == cachedShape", "location != null"},
        assumptions = "cachedShape.getValidAssumption()",
        limit = "LIMIT")
    protected final Object readSetField(final DynamicObject self,
        @Cached("self.getShape()") final Shape cachedShape,
        @Cached("getLocation(self)") final Location location) {
      return location.get(self, cachedShape);
    }

    @Specialization(guards = {"self.getShape() == cachedShape", "location == null"},
        assumptions = "cachedShape.getValidAssumption()",
        limit = "LIMIT")
    protected final Object readUnsetField(final DynamicObject self,
        @Cached("self.getShape()") final Shape cachedShape,
        @Cached("getLocation(self)") final Location location) {
      return Nil.nilObject;
    }

    @Specialization(guards = "self.updateShape()")
    public final Object updateShapeAndRead(final DynamicObject self) {
      return executeRead(self); // restart execution of the whole node
    }

//    @TruffleBoundary
    @Specialization(contains = {"readSetField", "readUnsetField", "updateShapeAndRead"})
    public final Object readFieldUncached(final DynamicObject receiver) {
      CompilerAsserts.neverPartOfCompilation("readFieldUncached");
      return receiver.get(fieldIndex, Nil.nilObject);
    }
  }

  public abstract static class WriteFieldNode extends FieldAccessorNode {
    public WriteFieldNode(final int fieldIndex) {
      super(fieldIndex);
    }

    public abstract Object executeWrite(DynamicObject obj, Object value);

    @Specialization(guards = {"self.getShape() == cachedShape", "location != null"},
        assumptions = {"locationAssignable", "cachedShape.getValidAssumption()"},
        limit = "LIMIT")
    public final Object writeFieldCached(final DynamicObject self,
        final Object value,
        @Cached("self.getShape()") final Shape cachedShape,
        @Cached("getLocation(self, value)") final Location location,
        @Cached("createAssumption()") final Assumption locationAssignable) {
      try {
        location.set(self, value);
      } catch (IncompatibleLocationException | FinalLocationException e) {
        // invalidate assumption to make sure this specialization gets removed
        locationAssignable.invalidate();
        return executeWrite(self, value); // restart execution for the whole node
      }
      return value;
    }

    @Specialization(guards = {"self.getShape() == oldShape", "oldLocation == null"},
        assumptions = {"locationAssignable", "oldShape.getValidAssumption()", "newShape.getValidAssumption()"},
        limit = "LIMIT")
    public final Object writeUnwrittenField(final DynamicObject self,
        final Object value,
        @Cached("self.getShape()") final Shape oldShape,
        @Cached("getLocation(self, value)") final Location oldLocation,
        @Cached("oldShape.defineProperty(fieldIndex, value, 0)") final Shape newShape,
        @Cached("newShape.getProperty(fieldIndex).getLocation()") final Location newLocation,
        @Cached("createAssumption()") final Assumption locationAssignable) {
      try {
        newLocation.set(self, value, oldShape, newShape);
      } catch (IncompatibleLocationException e) {
        // invalidate assumption to make sure this specialization gets removed
        locationAssignable.invalidate();
        return executeWrite(self, value); // restart execution for the whole node
      }
      return value;
    }

    @Specialization(guards = "self.updateShape()")
    public final Object updateObjectShapeAndRespecialize(
        final DynamicObject self, final Object value) {
      return executeWrite(self, value);
    }

    @Specialization(contains = {"writeFieldCached", "writeUnwrittenField", "updateObjectShapeAndRespecialize"})
    public final Object writeUncached(final DynamicObject self, final Object value) {
      self.define(fieldIndex, value);
      return value;
    }
  }
}
