package som.matenodes;

import som.interpreter.SArguments;
import som.matenodes.MateAbstractSemanticNodesFactory.MateEnvironmentSemanticCheckNodeGen;
import som.matenodes.MateAbstractSemanticNodesFactory.MateObjectSemanticCheckNodeGen;
import som.matenodes.MateAbstractSemanticNodesFactory.MateSemanticCheckNodeGen;
import som.vm.MateUniverse;
import som.vm.constants.ExecutionLevel;
import som.vm.constants.Nil;
import som.vm.constants.ReflectiveOp;
import som.vmobjects.SInvokable;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SObject;
import som.vmobjects.SReflectiveObject;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.object.Locations.ConstantLocation;
import com.oracle.truffle.object.ShapeImpl;
import com.oracle.truffle.object.basic.DynamicObjectBasic;

public abstract class MateAbstractSemanticNodes {

  public static abstract class MateEnvironmentSemanticCheckNode extends Node {
    private final ReflectiveOp reflectiveOperation;

    protected MateEnvironmentSemanticCheckNode(final ReflectiveOp operation) {
      super();
      reflectiveOperation = operation;
    }

    public abstract SInvokable executeGeneric(VirtualFrame frame);


    @Specialization(guards = "getEnvironment(frame) == null")
    public SInvokable doNoSemanticsInFrame(final VirtualFrame frame) {
      return null;
    }

    @Specialization(guards = {"getEnvironment(frame) == environment"})
    public SInvokable doSemanticsInFrame(final VirtualFrame frame,
        @Cached("getEnvironment(frame)") final DynamicObjectBasic environment,
        @Cached("methodImplementingOperationOn(environment)") final SInvokable reflectiveMethod) {
        return reflectiveMethod;
    }

    public static DynamicObjectBasic getEnvironment(final VirtualFrame frame){
      return SArguments.getEnvironment(frame);
    }

    public SInvokable methodImplementingOperationOn(final DynamicObjectBasic environment){
      return SMateEnvironment.methodImplementing(environment, this.reflectiveOperation);
    }

    @Override
    public NodeCost getCost() {
      return NodeCost.NONE;
    }
  }

  public static abstract class MateObjectSemanticCheckNode extends Node {

    protected final ReflectiveOp reflectiveOperation;

    protected MateObjectSemanticCheckNode(final ReflectiveOp operation) {
      super();
      reflectiveOperation = operation;
    }

    public abstract SInvokable executeGeneric(VirtualFrame frame,
        Object receiver);

    @Specialization(guards = "!isSReflectiveObject(receiver)")
    public SInvokable doStandardSOMForPrimitives(final VirtualFrame frame,
        final DynamicObjectBasic receiver) {
      return null;
    }

    @Specialization(guards = {"receiver.getShape().getRoot() == rootShape"}, limit = "10")
    public SInvokable doSReflectiveObject(
        final VirtualFrame frame,
        final DynamicObjectBasic receiver,
        @Cached("receiver.getShape().getRoot()") final ShapeImpl rootShape,
        @Cached("getEnvironmentLocationOf(receiver.getShape())") final ConstantLocation cachedLocation,
        @Cached("getEnvironment(receiver, cachedLocation)") final DynamicObjectBasic cachedEnvironment,
        @Cached("environmentReflectiveMethod(cachedEnvironment, reflectiveOperation)") final SInvokable method) {
      return method;
    }

    @Specialization(contains={"doSReflectiveObject", "doStandardSOMForPrimitives"})
    public SInvokable doMegamorphicReceiver(
        final VirtualFrame frame,
        final DynamicObjectBasic receiver) {
      return environmentReflectiveMethod(SReflectiveObject.getEnvironment(receiver), this.reflectiveOperation);
    }

    protected static SInvokable environmentReflectiveMethod(
        final DynamicObjectBasic environment, final ReflectiveOp operation) {
      if (environment == Nil.nilObject){
        return null;
      }
      return SMateEnvironment.methodImplementing(environment, operation);
    }

    public static DynamicObjectBasic getEnvironment(final DynamicObjectBasic receiver, final ConstantLocation location){
      return SObject.castDynObj(location.get(receiver));
    }

    public static ConstantLocation getEnvironmentLocationOf(final Shape shape){
      return (ConstantLocation) shape.getProperty(SReflectiveObject.ENVIRONMENT).getLocation();
    }

    public static boolean isSReflectiveObject(final DynamicObjectBasic object){
      return SReflectiveObject.isSReflectiveObject(object);
    }

    @Override
    public NodeCost getCost() {
      return NodeCost.NONE;
    }
  }

  public static abstract class MateSemanticCheckNode extends Node {

    @Child MateEnvironmentSemanticCheckNode environment;
    @Child MateObjectSemanticCheckNode      object;

    public abstract SInvokable execute(final VirtualFrame frame,
        Object[] arguments);

    public MateSemanticCheckNode(final MateEnvironmentSemanticCheckNode env,
        final MateObjectSemanticCheckNode obj) {
      super();
      environment = env;
      object = obj;
    }

    public static MateSemanticCheckNode createForFullCheck(
        final SourceSection source, final ReflectiveOp operation) {
      return MateSemanticCheckNodeGen.create(
          MateEnvironmentSemanticCheckNodeGen.create(operation),
          MateObjectSemanticCheckNodeGen.create(operation));
    }

    @Specialization(assumptions = "getMateDeactivatedAssumption()")
    protected SInvokable mateDeactivated(final VirtualFrame frame, final Object[] arguments) {
      return null;
    }

    @Specialization(guards = "!executeBase(frame)" ,
        assumptions = "getMateActivatedAssumption()")
    protected SInvokable executeSOM(final VirtualFrame frame, final Object[] arguments) {
      return null;
    }

    @Specialization(guards = "executeBase(frame)",
        assumptions = "getMateActivatedAssumption()")
    protected SInvokable executeSemanticChecks(final VirtualFrame frame,
        final Object[] arguments) {
      if (arguments[0] instanceof DynamicObjectBasic){
        SInvokable value = environment.executeGeneric(frame);
        if (value == null){
          value = object.executeGeneric(frame, arguments[0]);
        }
        return value;
      }
      return null;
    }

    public MateSemanticCheckNode(final SourceSection source,
        final ReflectiveOp operation) {
      super(source);
      environment = MateEnvironmentSemanticCheckNodeGen.create(operation);
      object = MateObjectSemanticCheckNodeGen.create(operation);
    }

    public static boolean executeBase(final VirtualFrame frame) {
      return SArguments.getExecutionLevel(frame) == ExecutionLevel.Base;
    }

    public static Assumption getMateDeactivatedAssumption() {
      return MateUniverse.current().getMateDeactivatedAssumption();
    }

    public static Assumption getMateActivatedAssumption() {
      return MateUniverse.current().getMateActivatedAssumption();
    }

    @Override
    public NodeCost getCost() {
      return NodeCost.NONE;
    }
  }
}
