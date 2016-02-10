package som.vm.constants;

import som.vm.MateUniverse;

import com.oracle.truffle.object.basic.DynamicObjectBasic;

public final class MateClasses extends Classes {
  public static final DynamicObjectBasic environmentMO;
  public static final DynamicObjectBasic operationalSemanticsMO;
  public static final DynamicObjectBasic messageMO;
  public static final DynamicObjectBasic ShapeClass;
  public static final DynamicObjectBasic STANDARD_ENVIRONMENT;

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