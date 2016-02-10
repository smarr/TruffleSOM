package som.vm.constants;

import som.vm.Universe;

import com.oracle.truffle.object.basic.DynamicObjectBasic;

public class Classes {
  public static final DynamicObjectBasic  objectClass;
  public static final DynamicObjectBasic  classClass;
  public static final DynamicObjectBasic  metaclassClass;

  public static final DynamicObjectBasic  nilClass;
  public static final DynamicObjectBasic  integerClass;
  public static final DynamicObjectBasic  arrayClass;
  public static final DynamicObjectBasic  methodClass;
  public static final DynamicObjectBasic  symbolClass;
  public static final DynamicObjectBasic  primitiveClass;
  public static final DynamicObjectBasic  stringClass;
  public static final DynamicObjectBasic  doubleClass;

  public static final DynamicObjectBasic  booleanClass;

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
