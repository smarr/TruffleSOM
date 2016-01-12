package som.vm.constants;

import com.oracle.truffle.api.object.DynamicObject;

import som.vm.MateUniverse;

public final class MateClasses extends Classes {
  public static final DynamicObject environmentMO;
  public static final DynamicObject operationalSemanticsMO;
  public static final DynamicObject messageMO;
  public static final DynamicObject ShapeClass;
  public static final DynamicObject STANDARD_ENVIRONMENT;
  
  static {
    // Allocate the Metaclass classes
    environmentMO = MateUniverse.newSystemClass();
    operationalSemanticsMO = MateUniverse.newSystemClass();
    messageMO = MateUniverse.newSystemClass();
    ShapeClass = MateUniverse.newSystemClass();
    STANDARD_ENVIRONMENT = null;
    //environment = Universe.newSystemClass();
  }
}