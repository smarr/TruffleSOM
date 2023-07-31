package trufflesom.vm.constants;

import trufflesom.vmobjects.SObject;


public final class Nil {
  public static final SObject nilObject;

  static {
    nilObject = SObject.create(0);
  }
}
