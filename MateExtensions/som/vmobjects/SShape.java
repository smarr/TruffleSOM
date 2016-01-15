package som.vmobjects;

import som.vm.constants.MateClasses;
import som.vm.constants.Nil;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;

public class SShape extends SAbstractObject {

  private Shape mockShape;

  @Override
  public DynamicObject getSOMClass() {
    return MateClasses.ShapeClass;
  }
  
  public SShape(int fieldsCount){
    Shape newShape = SObject.INIT_NIL_SHAPE.createSeparateShape(null);
    for (int i = 1; i < fieldsCount; i ++){
      newShape.defineProperty(i, Nil.nilObject, 0);
    }
    mockShape = newShape; 
  }
  
  public Shape getShape(){
    return mockShape;
  }
}
