package som.vmobjects;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;

import som.vm.constants.MateClasses;

public class SShape extends SAbstractObject {

  private DynamicObject mockObject;

  @Override
  public DynamicObject getSOMClass() {
    return MateClasses.ShapeClass;
  }
  
  public SShape(){
    mockObject = SReflectiveObject.create(0); 
  }
  
  public SShape(int fieldsCount){
    mockObject = SReflectiveObject.create(fieldsCount);
  }
  
  public Shape getShape(){
    return mockObject.getShape();
  }
}
