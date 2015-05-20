package som.vm;

import static som.vm.constants.MateClasses.environmentMO;
import static som.vm.constants.MateClasses.operationalSemanticsMO;
import static som.vm.constants.MateClasses.messageMO;
import static som.vm.constants.Classes.objectClass;

import com.oracle.truffle.api.RootCallTarget;

import som.interpreter.Invokable;
import som.interpreter.MateifyVisitor;
import som.vmobjects.SClass;
import som.vmobjects.SInvokable;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SReflectiveObject;
import som.vmobjects.SSymbol;

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
  
  public static SMateEnvironment newEnvironment(final SClass instanceClass) {
    return SMateEnvironment.create(instanceClass);
  }
  
  public SClass loadClass(final SSymbol name) {
    SClass result = super.loadClass(name);
    mateify(name);
    return result;
  }
  
  public void mateify(final SSymbol name) {
    SClass clazz = (SClass) getGlobal(name);
    Object[] invokables = clazz.getInstanceInvokables().getObjectStorage();
    MateifyVisitor visitor = new MateifyVisitor();
    for (int i = 0; i < invokables.length; i++){
      SInvokable method = (SInvokable) invokables[i];
      Invokable node = method.getInvokable();
      node.accept(visitor);
    }
  }
  
  public static Universe current() {
    if (current == null) {
      current = new MateUniverse();
    }
    return current;
  }
}
