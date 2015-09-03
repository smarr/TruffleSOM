package som.vmobjects;

import com.oracle.truffle.api.object.Shape;

import som.vm.constants.MateClasses;

public class SShape extends SAbstractObject {

  private SObject mockObject;

  @Override
  public SClass getSOMClass() {
    return MateClasses.ShapeClass;
  }
  
  public SShape(){
    mockObject = new SObject(0); 
  }
  
  public SShape(int fieldsCount){
    mockObject = new SObject(fieldsCount);
  }
  
  public Shape getShape(){
    return mockObject.getDynamicObject().getShape();
  }
}
