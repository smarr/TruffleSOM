package som.vm.constants;

import som.vmobjects.SObject;

import com.oracle.truffle.api.object.DynamicObject;


public final class Nil {
  public static final DynamicObject nilObject;

  static {
    nilObject = SObject.createNil();
  }
}
