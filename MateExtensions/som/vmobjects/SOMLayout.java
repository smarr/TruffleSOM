/*package som.vmobjects;

import java.lang.invoke.MethodHandles;
import java.util.EnumSet;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.ObjectLocation;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.object.Shape.Allocator;
import com.oracle.truffle.object.DynamicObjectImpl;
import com.oracle.truffle.object.LayoutImpl;
import com.oracle.truffle.object.LayoutStrategy;
import com.oracle.truffle.object.basic.BasicLocations.LongFieldLocation;
import com.oracle.truffle.object.basic.BasicLocations.ObjectFieldLocation;
import com.oracle.truffle.object.basic.BasicLocations.SimpleObjectFieldLocation;
import com.oracle.truffle.object.basic.ShapeBasic;

public class SOMLayout extends LayoutImpl {
  
  private final ObjectLocation[] objectFields;
  private final LongFieldLocation[] primitiveFields;
  private final Location objectArrayLocation;
  private final Location primitiveArrayLocation;
  
  protected SOMLayout(EnumSet<ImplicitCast> allowedImplicitCasts,
      Class<? extends DynamicObjectImpl> clazz, LayoutStrategy strategy) {
    super(allowedImplicitCasts, clazz, strategy);
    
    int index;
    java.lang.invoke.MethodHandles.Lookup lookup = MethodHandles.lookup();
    java.lang.reflect.Method[] declaredMethods = SOMDynamicObject.class.getDeclaredMethods();
    
    primitiveFields = new LongFieldLocation[SObjectOSM.NUM_PRIMITIVE_FIELDS];
    objectFields = new ObjectFieldLocation[SObjectOSM.NUM_OBJECT_FIELDS];
    
    for (index = 0; index < SObjectOSM.NUM_PRIMITIVE_FIELDS; index++){
      try {
        primitiveFields[index] = new LongFieldLocation(index, lookup.unreflect(declaredMethods[index]), lookup.unreflect(declaredMethods[index++]));
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
    
    for (index = 0; index < SObjectOSM.NUM_OBJECT_FIELDS; index++){
      try {
        objectFields[index] = new ObjectFieldLocation(index, lookup.unreflect(declaredMethods[index]), lookup.unreflect(declaredMethods[index++]));
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
    
    objectArrayLocation = new SimpleObjectFieldLocation(SObjectOSM.NUM_OBJECT_FIELDS) {
      @Override
      public Object[] get(DynamicObject store, boolean condition) {
          return ((SOMDynamicObject) store).getExtensionObjectFields();
      }

      @Override
      public void setInternal(DynamicObject store, Object value) {
          ((SOMDynamicObject) store).setExtensionObjectFields((Object[]) value);
      }
    };

    primitiveArrayLocation = new SimpleObjectFieldLocation(SObjectOSM.NUM_OBJECT_FIELDS + 1) {
      @Override
      public long[] get(DynamicObject store, boolean condition) {
        return ((SOMDynamicObject) store).getExtensionPrimitiveFields();
      }

      @Override
      public void setInternal(DynamicObject store, Object value) {
        ((SOMDynamicObject) store).setExtensionPrimitiveFields((long[]) value);
      }
    };
  }
  
  @Override
  public DynamicObject newInstance(Shape shape) {
      return new SOMDynamicObject(shape);
  }
  
  /*@Override
  public Shape createShape(ObjectType operations, Object sharedData, int id) {
      return new ShapeBasic(this, sharedData, operations, id);
  }

  @Override
  protected boolean hasObjectExtensionArray() {
    return true;
  }

  @Override
  protected boolean hasPrimitiveExtensionArray() {
    return true;
  }

  @Override
  protected int getObjectFieldCount() {
    return objectFields.length;
  }

  @Override
  protected int getPrimitiveFieldCount() {
    return primitiveFields.length;
  }

  @Override
  protected Location getObjectArrayLocation() {
    return objectArrayLocation;
  }

  @Override
  protected Location getPrimitiveArrayLocation() {
    return primitiveArrayLocation;
  }

  @Override
  protected int objectFieldIndex(Location location) {
    if (location instanceof LongFieldLocation)
        return ((LongFieldLocation) location).getIndex();
    else
      return ((ObjectFieldLocation) location).getIndex() + SObjectOSM.NUM_PRIMITIVE_FIELDS;
  }

  @Override
  public Allocator createAllocator() {
    LayoutImpl layout = this;
    Allocator allocator = getStrategy().createAllocator(layout);
    return allocator;
  }

  @Override
  public Shape createShape(ObjectType operations, Object sharedData, int id) {
    return new ShapeBasic(this, sharedData, operations, id);
  }
}
*/