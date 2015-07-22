package som.vmobjects;

import som.vm.Universe;
import som.vm.constants.Nil;
import som.vm.constants.ReflectiveOp;
import som.vmobjects.SInvokable.SMethod;


public class SMateEnvironment extends SObject {
  public static final int Semantics_IDX = 0;
  public static final int Message_IDX = 2;
  
  public static SMateEnvironment create(final SClass instanceClass) {
    return new SMateEnvironment(instanceClass);
  }
  
  public SMateEnvironment(SClass instanceClass) {
    super(instanceClass);
  }

  protected boolean reimplementsOperation(ReflectiveOp operation){
    return methodImplementing(operation) != null;
  }
  
  public SMethod methodImplementing(ReflectiveOp operation){
    Object metaobject = null;
    switch (operation){
      case None: 
        return null;
      case Lookup: 
        metaobject = this.getField(Message_IDX);
        break;
      case ReadField: case WriteField: 
        metaobject = this.getField(Semantics_IDX);
    }
    if (metaobject == Nil.nilObject) return null;
    return this.methodForOperation((SObject)metaobject, operation);
  }
  
  @SuppressWarnings("incomplete-switch")
  private SMethod methodForOperation(SObject metaobject, ReflectiveOp operation){
    SSymbol methodName = null;
    switch (operation){
      case Lookup:
        methodName = Universe.current().symbolFor("find:since");
        break;
      case ReadField: 
        methodName = Universe.current().symbolFor("read");
        break;
      case WriteField: 
        methodName = Universe.current().symbolFor("write:");
        break;
    }
    SInvokable method = metaobject.getSOMClass().lookupInvokable(methodName);
    return (SMethod)method;
  }
}