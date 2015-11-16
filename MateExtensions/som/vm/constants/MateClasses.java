package som.vm.constants;

import som.vm.MateUniverse;
import som.vmobjects.SClass;
import som.vmobjects.SMateEnvironment;

public class MateClasses extends Classes {
  public static final SClass environmentMO;
  public static final SClass operationalSemanticsMO;
  public static final SClass messageMO;
  public static final SClass ShapeClass;
  public static final SMateEnvironment STANDARD_ENVIRONMENT;
  
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