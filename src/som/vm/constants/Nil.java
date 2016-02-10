package som.vm.constants;

import som.vmobjects.SObject;

import com.oracle.truffle.object.basic.DynamicObjectBasic;


public final class Nil {
  public static final DynamicObjectBasic nilObject;

  static {
    nilObject = SObject.createNil();
  }
}
