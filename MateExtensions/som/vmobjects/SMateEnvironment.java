package som.vmobjects;

import som.vm.Universe;
import som.vm.constants.Nil;
import som.vm.constants.ReflectiveOp;

import com.oracle.truffle.api.object.DynamicObject;

public class SMateEnvironment extends SObject {
  //private static final SMateEnvironmentObjectType SMATE_ENVIRONMENT_TYPE = new SMateEnvironmentObjectType();

  public static final int Semantics_IDX = 0;
  public static final int Layout_IDX = 1;
  public static final int Message_IDX = 2;

  /*
  protected static final SSymbol LAYOUT = Universe.current().symbolFor("layout");
  protected static final SSymbol MESSAGE = Universe.current().symbolFor("message");

  protected static final Shape SMATE_ENVIRONMENT_OBJECT_SHAPE =
      SOBJECT_SHAPE.createSeparateShape(SOBJECT_SHAPE.getData()).defineProperty(SEMANTICS, Nil.nilObject, 0).defineProperty(LAYOUT, Nil.nilObject, 0).defineProperty(MESSAGE, Nil.nilObject, 0);

  private static final DynamicObjectFactory SMATE_ENVIRONMENT_OBJECT_FACTORY = SMATE_ENVIRONMENT_OBJECT_SHAPE.createFactory();

  public static DynamicObject create(final DynamicObject instanceClass) {
    return SMATE_ENVIRONMENT_OBJECT_FACTORY.newInstance(instanceClass, Nil.nilObject, Nil.nilObject, Nil.nilObject);
  }

  public static boolean isSMateEnvironment(final DynamicObject obj) {
    return obj.getShape().getObjectType() == SMATE_ENVIRONMENT_TYPE;
  }

  private static final class SMateEnvironmentObjectType extends ObjectType {
    @Override
    public String toString() {
      return "SMateEnvironment";
    }
  }
  */

  //Todo: Finish the SMateEnvironment type with primitives for setting it fields
  public static SInvokable methodImplementing(final DynamicObject obj, final ReflectiveOp operation){
    int field;
    switch (operation){
      case None:
        return null;
      case MessageLookup: case MessageActivation:
        field = Message_IDX;
        break;
      case ExecutorReadField: case ExecutorWriteField:
        field = Semantics_IDX;
        break;
      case LayoutReadField: case LayoutWriteField:
        field = Layout_IDX;
        break;
      default:
        return null;
    }
    DynamicObject metaobject = SObject.castDynObj(obj.get(field));
    if (metaobject == Nil.nilObject) {
      return null;
    }
    return methodForOperation(metaobject, operation);
  }

  /*Optimize this method. It can have the definition of the symbols in a static ahead of time  way*/
  private static SInvokable methodForOperation(final DynamicObject metaobject, final ReflectiveOp operation){
    SSymbol selector;
    switch (operation){
      case MessageLookup:
        selector = Universe.current().symbolFor("find:since:");
        break;
      case MessageActivation:
        selector = Universe.current().symbolFor("activate:withArguments:");
        break;
      case ExecutorReadField:
        selector = Universe.current().symbolFor("read:");
        break;
      case ExecutorWriteField:
        selector = Universe.current().symbolFor("write:value:");
        break;
      case LayoutReadField:
        selector = Universe.current().symbolFor("read:");
        break;
      case LayoutWriteField:
        selector = Universe.current().symbolFor("write:value:");
        break;
      default:
        selector = null;
    }
    return SClass.lookupInvokable(SObject.getSOMClass(metaobject), selector);
  }
}