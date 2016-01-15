package som.vm.constants;

import som.vm.Universe;

import com.oracle.truffle.api.object.DynamicObject;



public final class Globals {
  public static final DynamicObject trueObject;
  public static final DynamicObject falseObject;
  public static final DynamicObject systemObject;

  public static final DynamicObject trueClass;
  public static final DynamicObject falseClass;
  public static final DynamicObject systemClass;

 static {
    trueObject   = Universe.current().getTrueObject();
    falseObject  = Universe.current().getFalseObject();
    systemObject = Universe.current().getSystemObject();

    trueClass   = Universe.current().getTrueClass();
    falseClass  = Universe.current().getFalseClass();
    systemClass = Universe.current().getSystemClass();
  }
}
