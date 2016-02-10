package som.vmobjects;

import som.vm.constants.MateClasses;
import som.vm.constants.Nil;

import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.object.basic.DynamicObjectBasic;

public class SShape extends SAbstractObject {

  private Shape mockShape;

  @Override
  public DynamicObjectBasic getSOMClass() {
    return MateClasses.ShapeClass;
  }

  public SShape(final int fieldsCount){
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
