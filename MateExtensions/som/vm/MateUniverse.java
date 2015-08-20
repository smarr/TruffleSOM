package som.vm;

import static som.vm.constants.MateClasses.environmentMO;
import static som.vm.constants.MateClasses.operationalSemanticsMO;
import static som.vm.constants.MateClasses.messageMO;
import static som.vm.constants.Classes.objectClass;

import som.interpreter.Invokable;
import som.interpreter.MateifyVisitor;
import som.vmobjects.SClass;
import som.vmobjects.SInvokable;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SReflectiveObject;
import som.vmobjects.SSymbol;

public class MateUniverse extends Universe {
  
  protected boolean executingMeta;
  
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
      
      this.executingMeta = false;
    }
  }
  
  public static SReflectiveObject newInstance(final SClass instanceClass) {
    return SReflectiveObject.create(instanceClass);
  }
  
  public static SMateEnvironment newEnvironment(final SClass instanceClass) {
    return SMateEnvironment.create(instanceClass);
  }
  
  public SClass loadClass(final SSymbol name) {
    if ((SClass) getGlobal(name) != null){
      return super.loadClass(name);
    } else {
      SClass result = super.loadClass(name);
      mateify(name);
      return result;
    }
  }
  
  public void mateify(final SSymbol name) {
    SClass clazz = (SClass) getGlobal(name);
    int countOfInvokables = clazz.getNumberOfInstanceInvokables();
    MateifyVisitor visitor = new MateifyVisitor();
    for (int i = 0; i < countOfInvokables; i++){
      SInvokable method = clazz.getInstanceInvokable(i);
      Invokable node = method.getInvokable();
      node.accept(visitor);
    }
  }
  
  public static MateUniverse current() {
    if (Universe.getCurrent() == null) {
      Universe.setCurrent(new MateUniverse());
    }
    return (MateUniverse) Universe.getCurrent();
  }
  
  public boolean executingMeta() {
    return executingMeta;
  }
  
  public void enterMetaExecutionLevel() {
    this.executingMeta = true;
  }
  
  public void leaveMetaExecutionLevel() {
    this.executingMeta = false;
  }
}
