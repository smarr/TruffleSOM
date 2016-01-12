package som.vm;

import static som.vm.constants.MateClasses.environmentMO;
import static som.vm.constants.MateClasses.operationalSemanticsMO;
import static som.vm.constants.MateClasses.messageMO;
import static som.vm.constants.MateClasses.ShapeClass;
import static som.vm.constants.Classes.objectClass;

import com.oracle.truffle.api.object.DynamicObject;

import som.interpreter.Invokable;
import som.interpreter.MateifyVisitor;
import som.interpreter.nodes.MateMessageSpecializationsFactory;
import som.interpreter.nodes.MessageSendNode.AbstractMessageSendNode;
import som.vmobjects.SClass;
import som.vmobjects.SInvokable;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SObject;
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
      initializeSystemClass(ShapeClass, objectClass, "Shape");
      
      // Load methods and fields into the Mate MOP.
      loadSystemClass(environmentMO);
      loadSystemClass(operationalSemanticsMO);
      loadSystemClass(messageMO);
      loadSystemClass(ShapeClass);
      
      AbstractMessageSendNode.specializationFactory = new MateMessageSpecializationsFactory();
    }
  }
  
  public static DynamicObject newInstance(final DynamicObject instanceClass) {
    return SReflectiveObject.create(instanceClass);
  }
  
  public static DynamicObject newEnvironment(final DynamicObject instanceClass) {
    return SMateEnvironment.create(instanceClass);
  }
  
  @Override
  public DynamicObject loadClass(final SSymbol name) {
    if ((DynamicObject) getGlobal(name) != null){
      return super.loadClass(name);
    } else {
      DynamicObject result = super.loadClass(name);
      mateify(result);
      mateify(SObject.getSOMClass(result));
      return result;
    }
  }
  
  protected void loadSystemClass(final DynamicObject systemClass) {
    super.loadSystemClass(systemClass);
    mateify(systemClass);
    mateify(SObject.getSOMClass(systemClass));
  }
  
  public void mateify(DynamicObject clazz) {
    int countOfInvokables = SClass.getNumberOfInstanceInvokables(clazz);
    MateifyVisitor visitor = new MateifyVisitor();
    for (int i = 0; i < countOfInvokables; i++){
      SInvokable method = SClass.getInstanceInvokable(clazz, i);
      Invokable node = method.getInvokable();
      node.accept(visitor);
    }
  }
  
  public void mateifyMethod(SInvokable method) {
    MateifyVisitor visitor = new MateifyVisitor();
    Invokable node = method.getInvokable();
    node.accept(visitor);
  }
  
  public static void main(final String[] arguments) {
    MateUniverse u = current();
    try {
      u.interpret(arguments);
      u.exit(0);
    } catch (IllegalStateException e) {
      errorExit(e.getMessage());
    }
  }
  
  public static MateUniverse current() {
    if (Universe.getCurrent() == null) {
      Universe.setCurrent(new MateUniverse());
    }
    return (MateUniverse) Universe.getCurrent();
  }
}