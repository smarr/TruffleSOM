package som.vm.constants;

import som.vm.Universe;

import com.oracle.truffle.object.basic.DynamicObjectBasic;



public final class Globals {
  public static final DynamicObjectBasic trueObject;
  public static final DynamicObjectBasic falseObject;
  public static final DynamicObjectBasic systemObject;

  public static final DynamicObjectBasic trueClass;
  public static final DynamicObjectBasic falseClass;
  public static final DynamicObjectBasic systemClass;

 static {
    trueObject   = Universe.current().getTrueObject();
    falseObject  = Universe.current().getFalseObject();
    systemObject = Universe.current().getSystemObject();

    trueClass   = Universe.current().getTrueClass();
    falseClass  = Universe.current().getFalseClass();
    systemClass = Universe.current().getSystemClass();
  }
}
