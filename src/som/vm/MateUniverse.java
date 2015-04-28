package som.vm;

import static som.vm.constants.MateClasses.environmentMO;
import static som.vm.constants.MateClasses.operationalSemanticsMO;
import static som.vm.constants.MateClasses.messageMO;
import static som.vm.constants.Classes.objectClass;
import som.vmobjects.SClass;
import som.vmobjects.SReflectiveObject;

public class MateUniverse extends Universe {
  
  protected void initializeObjectSystem() {
    if (alreadyInitialized) {
      return;
    } else {
      super.initializeObjectSystem();
      
      // Initialize the Mate metamodel.
      initializeSystemClass(environmentMO, objectClass, "EnvironmentMO");
      initializeSystemClass(operationalSemanticsMO, objectClass, "OperationalSemanticsMO");
      initializeSystemClass(messageMO, objectClass, "MessageMO");
      
      // Load methods and fields into the Mate MOP.
      loadSystemClass(environmentMO);
      loadSystemClass(operationalSemanticsMO);
      loadSystemClass(messageMO);
    }
  }
  
  public static SReflectiveObject newInstance(final SClass instanceClass) {
    return SReflectiveObject.create(instanceClass);
  }
  
  public static Universe current() {
    if (current == null) {
      current = new MateUniverse();
    }
    return current;
  }
}
