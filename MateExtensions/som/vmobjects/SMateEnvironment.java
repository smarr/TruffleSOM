package som.vmobjects;

import som.vm.Universe;
import som.vm.constants.Nil;
import som.vm.constants.ReflectiveOp;
import som.vmobjects.SInvokable.SMethod;


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

  protected boolean reimplementsOperation(ReflectiveOp operation){
    return methodsImplementing(operation).length > 0;
  }
  
  public SMethod[] methodsImplementing(ReflectiveOp operation){
    Object metaobject = null;
    switch (operation){
      case None: 
        return null;
      case Lookup: 
        metaobject = this.getField(Message_IDX);
        break;
      case ReadField: case WriteField: 
        metaobject = this.getField(Semantics_IDX);
        break;
      case ReadLayout: case WriteLayout: 
        metaobject = this.getField(Layout_IDX);  
        break;
    }
    if (metaobject == Nil.nilObject) return null;
    return this.methodForOperation((SObject)metaobject, operation);
  }
  
  /*Optimize this method. It can have the definition of the symbols in a static ahead of time  way*/
  private SMethod[] methodForOperation(SObject metaobject, ReflectiveOp operation){
    SMethod[] methods;
    switch (operation){
      case Lookup:
        methods = new SMethod[2];
        methods[0] = (SMethod)metaobject.getSOMClass().lookupInvokable(Universe.current().symbolFor("find:since:"));
        methods[1] = (SMethod)metaobject.getSOMClass().lookupInvokable(Universe.current().symbolFor("activate:withArguments:"));
        if (methods[0] == null && methods[1] == null) return null;
        break;
      case ReadField: 
        methods = new SMethod[1];
        methods[0] = (SMethod)metaobject.getSOMClass().lookupInvokable(Universe.current().symbolFor("read:"));
        break;
      case WriteField: 
        methods = new SMethod[1];
        methods[0] = (SMethod)metaobject.getSOMClass().lookupInvokable(Universe.current().symbolFor("write:value:"));
        break;
      default:
        methods = new SMethod[0];
        break;
    }
    if (methods[0] == null) return null;
    return methods;
  }
}