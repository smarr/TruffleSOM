package trufflesom.vm;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import trufflesom.vmobjects.SClass;


public class Classes {
  public static final SClass objectClass;
  public static final SClass classClass;
  public static final SClass metaclassClass;

  public static final SClass nilClass;
  public static final SClass integerClass;
  public static final SClass arrayClass;
  public static final SClass methodClass;
  public static final SClass symbolClass;
  public static final SClass primitiveClass;
  public static final SClass stringClass;
  public static final SClass doubleClass;

  public static final SClass booleanClass;

  public static final SClass trueClass;
  public static final SClass falseClass;

  static {
    // Allocate the Metaclass classes
    metaclassClass = newMetaclassClass();

    // Allocate the rest of the system classes

    objectClass = newSystemClass();
    nilClass = newSystemClass();
    classClass = newSystemClass();
    arrayClass = newSystemClass();
    symbolClass = newSystemClass();
    methodClass = newSystemClass();
    integerClass = newSystemClass();
    primitiveClass = newSystemClass();
    stringClass = newSystemClass();
    doubleClass = newSystemClass();
    booleanClass = newSystemClass();

    trueClass = newSystemClass();
    falseClass = newSystemClass();
  }

  @TruffleBoundary
  public static SClass newMetaclassClass() {
    // Allocate the metaclass classes
    SClass result = new SClass(0);
    result.setClass(new SClass(0));

    // Setup the metaclass hierarchy
    result.getSOMClass(null).setClass(result);
    return result;
  }

  @TruffleBoundary
  public static SClass newSystemClass() {
    // Allocate the new system class
    SClass systemClass = new SClass(0);

    // Setup the metaclass hierarchy
    systemClass.setClass(new SClass(0));
    systemClass.getSOMClass().setClass(metaclassClass);

    // Return the freshly allocated system class
    return systemClass;
  }

  public static void reset() {
    metaclassClass.resetMetaclassClass();
    objectClass.resetSystemClass();
    nilClass.resetSystemClass();
    classClass.resetSystemClass();
    arrayClass.resetSystemClass();
    symbolClass.resetSystemClass();
    methodClass.resetSystemClass();
    integerClass.resetSystemClass();
    primitiveClass.resetSystemClass();
    stringClass.resetSystemClass();
    doubleClass.resetSystemClass();
    booleanClass.resetSystemClass();

    trueClass.resetSystemClass();
    falseClass.resetSystemClass();
  }
}
