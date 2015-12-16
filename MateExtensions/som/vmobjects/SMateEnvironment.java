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
      case MessageLookup: case MessageActivation: 
        metaobject = this.getField(Message_IDX);
        break;
      case ExecutorReadField: case ExecutorWriteField: 
        metaobject = this.getField(Semantics_IDX);
        break;
      case LayoutReadField: case LayoutWriteField: 
        metaobject = this.getField(Layout_IDX);  
        break;
      default:
        break;
    }
    if (metaobject == Nil.nilObject) throw new MateSemanticsException();
    return this.methodForOperation((SObject)metaobject, operation);
  }
  
  /*Optimize this method. It can have the definition of the symbols in a static ahead of time  way*/
  private SInvokable methodForOperation(SObject metaobject, ReflectiveOp operation){
    SInvokable method; 
    switch (operation){
      case MessageLookup:
        method = metaobject.getSOMClass().lookupInvokable(Universe.current().symbolFor("find:since:"));
        break;
      case MessageActivation:  
        method = metaobject.getSOMClass().lookupInvokable(Universe.current().symbolFor("activate:withArguments:"));
        break;
      case ExecutorReadField: 
        method = metaobject.getSOMClass().lookupInvokable(Universe.current().symbolFor("read:"));
        break;
      case ExecutorWriteField: 
        method = metaobject.getSOMClass().lookupInvokable(Universe.current().symbolFor("write:value:"));
        break;
      case LayoutReadField: 
        method = metaobject.getSOMClass().lookupInvokable(Universe.current().symbolFor("read:"));
        break;
      case LayoutWriteField: 
        method = metaobject.getSOMClass().lookupInvokable(Universe.current().symbolFor("write:value:"));
        break;
      default:
        method = null;
    }
    if (method == null) throw new MateSemanticsException();
    return method;
  }
}