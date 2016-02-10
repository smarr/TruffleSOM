package som.vm;

import static som.vm.constants.Classes.objectClass;
import static som.vm.constants.MateClasses.ShapeClass;
import static som.vm.constants.MateClasses.environmentMO;
import static som.vm.constants.MateClasses.messageMO;
import static som.vm.constants.MateClasses.operationalSemanticsMO;
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

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.object.basic.DynamicObjectBasic;

public class MateUniverse extends Universe {
  private Assumption mateActivated;
  private Assumption mateDeactivated;

  public MateUniverse() {
    super();
    mateDeactivated = this.getTruffleRuntime().createAssumption();
    mateActivated = null;
  }

  @Override
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
      //SObject.internalSetNilClass(nilObject, nilClass);

      // Load methods and fields into the Mate MOP.
      loadSystemClass(environmentMO);
      loadSystemClass(operationalSemanticsMO);
      loadSystemClass(messageMO);
      loadSystemClass(ShapeClass);

      AbstractMessageSendNode.specializationFactory = new MateMessageSpecializationsFactory();
    }
  }

  public static DynamicObjectBasic newInstance(final DynamicObjectBasic instanceClass) {
    return SReflectiveObject.create(instanceClass);
  }

  public static DynamicObjectBasic newEnvironment(final DynamicObjectBasic instanceClass) {
    return SMateEnvironment.create(instanceClass);
  }

  @Override
  public DynamicObjectBasic loadClass(final SSymbol name) {
    CompilerAsserts.neverPartOfCompilation("loadClass");
    if ((DynamicObjectBasic) getGlobal(name) != null){
      return super.loadClass(name);
    } else {
      DynamicObjectBasic result = super.loadClass(name);
      mateify(result);
      mateify(SObject.getSOMClass(result));
      return result;
    }
  }

  @Override
  protected void loadSystemClass(final DynamicObjectBasic systemClass) {
    super.loadSystemClass(systemClass);
    mateify(systemClass);
    mateify(SObject.getSOMClass(systemClass));
  }

  public void mateify(final DynamicObjectBasic clazz) {
    int countOfInvokables = SClass.getNumberOfInstanceInvokables(clazz);
    MateifyVisitor visitor = new MateifyVisitor();
    for (int i = 0; i < countOfInvokables; i++){
      SInvokable method = SClass.getInstanceInvokable(clazz, i);
      Invokable node = method.getInvokable();
      node.accept(visitor);
    }
  }

  public void mateifyMethod(final SInvokable method) {
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

  @Override
  public DynamicObjectFactory getInstancesFactory(){
    return SReflectiveObject.SREFLECTIVE_OBJECT_FACTORY;
  }

  @Override
  public Shape createObjectShapeForClass(final DynamicObjectBasic clazz){
    return SReflectiveObject.createObjectShapeForClass(clazz);
  }

  public Assumption getMateDeactivatedAssumption(){
    return this.mateDeactivated;
  }

  public Assumption getMateActivatedAssumption(){
    return this.mateActivated;
  }

  @Override
  public void activatedMate(){
    if (this.getMateDeactivatedAssumption().isValid()){
      this.getMateDeactivatedAssumption().invalidate();
    }
    this.mateActivated = this.getTruffleRuntime().createAssumption();
  }

  public void deactivateMate(){
    if (this.getMateActivatedAssumption().isValid()){
      this.getMateActivatedAssumption().invalidate();
    }
    this.mateDeactivated = this.getTruffleRuntime().createAssumption();
  }

  public static MateUniverse current() {
    if (Universe.getCurrent() == null) {
      Universe.setCurrent(new MateUniverse());
    }
    return (MateUniverse) Universe.getCurrent();
  }
}