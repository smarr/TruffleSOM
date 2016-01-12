package som.vm.constants;

import som.vm.Universe;

import com.oracle.truffle.api.object.DynamicObject;

public class Classes {
  public static final DynamicObject  objectClass;
  public static final DynamicObject  classClass;
  public static final DynamicObject  metaclassClass;

  public static final DynamicObject  nilClass;
  public static final DynamicObject  integerClass;
  public static final DynamicObject  arrayClass;
  public static final DynamicObject  methodClass;
  public static final DynamicObject  symbolClass;
  public static final DynamicObject  primitiveClass;
  public static final DynamicObject  stringClass;
  public static final DynamicObject  doubleClass;

  public static final DynamicObject  booleanClass;

  // These classes can be statically preinitialized.
  static {
    // Allocate the Metaclass classes
    metaclassClass = Universe.newMetaclassClass();

    // Allocate the rest of the system classes

    objectClass     = Universe.newSystemClass();
    nilClass        = Universe.newSystemClass();
    classClass      = Universe.newSystemClass();
    arrayClass      = Universe.newSystemClass();
    symbolClass     = Universe.newSystemClass();
    methodClass     = Universe.newSystemClass();
    integerClass    = Universe.newSystemClass();
    primitiveClass  = Universe.newSystemClass();
    stringClass     = Universe.newSystemClass();
    doubleClass     = Universe.newSystemClass();
    booleanClass    = Universe.newSystemClass();
  }
}
