package som.vmobjects;

import som.vm.MateSemanticsException;
import som.vm.Universe;
import som.vm.constants.Nil;
import som.vm.constants.ReflectiveOp;


public class SMateEnvironment extends SObject {
  public static final int Semantics_IDX = 0;
  public static final int Layout_IDX = 1;
  public static final int Message_IDX = 2;
  
  public static SMateEnvironment create(final SClass instanceClass) {
    return new SMateEnvironment(instanceClass);
  }
  
  public SMateEnvironment(SClass instanceClass) {
    super(instanceClass);
  }

  public SInvokable methodImplementing(ReflectiveOp operation){
    Object metaobject = null;
    switch (operation){
      case None: 
        throw new MateSemanticsException();
      case Lookup: case Activation: 
        metaobject = this.getField(Message_IDX);
        break;
      case ReadField: case WriteField: 
        metaobject = this.getField(Semantics_IDX);
        break;
      case ReadLayout: case WriteLayout: 
        metaobject = this.getField(Layout_IDX);  
        break;
    }
    if (metaobject == Nil.nilObject) throw new MateSemanticsException();
    return this.methodForOperation((SObject)metaobject, operation);
  }
  
  /*Optimize this method. It can have the definition of the symbols in a static ahead of time  way*/
  private SInvokable methodForOperation(SObject metaobject, ReflectiveOp operation){
    switch (operation){
      case Lookup:
        return metaobject.getSOMClass().lookupInvokable(Universe.current().symbolFor("find:since:"));
      case Activation:  
        return metaobject.getSOMClass().lookupInvokable(Universe.current().symbolFor("activate:withArguments:"));
      case ReadField: 
        return metaobject.getSOMClass().lookupInvokable(Universe.current().symbolFor("read:"));
      case WriteField: 
        return metaobject.getSOMClass().lookupInvokable(Universe.current().symbolFor("write:value:"));
      case ReadLayout: 
        return metaobject.getSOMClass().lookupInvokable(Universe.current().symbolFor("read:"));
      case WriteLayout: 
        return metaobject.getSOMClass().lookupInvokable(Universe.current().symbolFor("write:value:"));
      default:
        return null;
    }
  }
}