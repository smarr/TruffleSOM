package trufflesom.vm.constants;

import trufflesom.vmobjects.SObject;

import com.oracle.truffle.api.object.DynamicObject;


public final class Nil {
  public static final DynamicObject nilObject;

  static {
    nilObject = SObject.createNil();
  }
}
