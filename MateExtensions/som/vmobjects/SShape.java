package som.vmobjects;

import com.oracle.truffle.api.object.Shape;

import som.vm.constants.Classes;

public class SShape extends SAbstractObject {

  private SObject mockObject;

  @Override
  public SClass getSOMClass() {
    return Classes.arrayClass;
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
