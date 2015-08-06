package som.vmobjects;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.object.DynamicObjectImpl;

public class SOMDynamicObject extends DynamicObjectImpl {
  @Retention(RetentionPolicy.RUNTIME)
  protected @interface DynamicField {
  }
  
  @DynamicField private long primitive1;
  @DynamicField private long primitive2;
  @DynamicField private long primitive3;
  @DynamicField private long primitive4;
  @DynamicField private long primitive5;
  @DynamicField private Object object1;
  @DynamicField private Object object2;
  @DynamicField private Object object3;
  @DynamicField private Object object4;
  @DynamicField private Object object5;
  private Object[] extensionObjectFields;
  private long[] extensionPrimitiveFields;
  
  public SOMDynamicObject(Shape shape) {
    super(shape);
  }

  @Override
  protected void initialize(Shape initialShape) {
    // TODO Auto-generated method stub
    
  }

  @Override
  protected void growObjectStore(Shape oldShape, Shape newShape) {
    // TODO Auto-generated method stub
    
  }

  @Override
  protected void growPrimitiveStore(Shape oldShape, Shape newShape) {
    // TODO Auto-generated method stub
    
  }

  @Override
  protected void resizePrimitiveStore(Shape oldShape, Shape newShape) {
    // TODO Auto-generated method stub
    
  }

  @Override
  protected void resizeObjectStore(Shape oldShape, Shape newShape) {
    // TODO Auto-generated method stub
    
  }

  @Override
  protected boolean checkExtensionArrayInvariants(Shape newShape) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  protected DynamicObject cloneWithShape(Shape currentShape) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @ExplodeLoop
  public List<String> getKeys(){
    List<Property> properties = this.getShape().getPropertyList();
    List<String> keys = new ArrayList<String>(this.size());
    for (int i = 0; i < this.size(); i++) {
      keys.add(i, (String) properties.get(i).getKey());
    }
    return keys;
  }
  
  @ExplodeLoop
  public List<Object> getValues(){
    List<String> keys = this.getKeys();
    List<Object> values = new ArrayList<Object>(keys.size());
    for (String key: keys) {
        values.add(this.get(key, null));
    }
    return values;
  }

  public long getPrimitive1() {
    return primitive1;
  }

  public void setPrimitive1(long primitive1) {
    this.primitive1 = primitive1;
  }

  public long getPrimitive2() {
    return primitive2;
  }

  public void setPrimitive2(long primitive2) {
    this.primitive2 = primitive2;
  }

  public long getPrimitive3() {
    return primitive3;
  }

  public void setPrimitive3(long primitive3) {
    this.primitive3 = primitive3;
  }

  public long getPrimitive4() {
    return primitive4;
  }

  public void setPrimitive4(long primitive4) {
    this.primitive4 = primitive4;
  }

  public long getPrimitive5() {
    return primitive5;
  }

  public void setPrimitive5(long primitive5) {
    this.primitive5 = primitive5;
  }

  public Object getObject1() {
    return object1;
  }

  public void setObject1(Object object1) {
    this.object1 = object1;
  }

  public Object getObject2() {
    return object2;
  }

  public void setObject2(Object object2) {
    this.object2 = object2;
  }

  public Object getObject3() {
    return object3;
  }

  public void setObject3(Object object3) {
    this.object3 = object3;
  }

  public Object getObject4() {
    return object4;
  }

  public void setObject4(Object object4) {
    this.object4 = object4;
  }

  public Object getObject5() {
    return object5;
  }

  public void setObject5(Object object5) {
    this.object5 = object5;
  }

  public Object[] getExtensionObjectFields() {
    return extensionObjectFields;
  }

  public void setExtensionObjectFields(Object[] extensionObjectFields) {
    this.extensionObjectFields = extensionObjectFields;
  }

  public long[] getExtensionPrimitiveFields() {
    return extensionPrimitiveFields;
  }

  public void setExtensionPrimitiveFields(long[] extensionPrimitiveFields) {
    this.extensionPrimitiveFields = extensionPrimitiveFields;
  }
  
  /*private Object[] getObjectStore(@SuppressWarnings("unused") Shape currentShape) {
    return extensionObjectFields;
  }

  private void setObjectStore(Object[] newArray, @SuppressWarnings("unused") Shape currentShape) {
    extensionObjectFields = newArray;
  }

  private long[] getPrimitiveStore(@SuppressWarnings("unused") Shape currentShape) {
    return extensionPrimitiveFields;
  }

  private void setPrimitiveStore(long[] newArray, @SuppressWarnings("unused") Shape currentShape) {
    extensionPrimitiveFields = newArray;
  }*/
}
