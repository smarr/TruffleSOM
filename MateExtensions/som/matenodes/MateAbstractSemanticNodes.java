package som.matenodes;

import som.interpreter.SArguments;
import som.matenodes.MateAbstractSemanticNodesFactory.MateEnvironmentSemanticCheckNodeGen;
import som.matenodes.MateAbstractSemanticNodesFactory.MateObjectSemanticCheckNodeGen;
import som.matenodes.MateAbstractSemanticNodesFactory.MateSemanticCheckNodeGen;
import som.vm.MateSemanticsException;
import som.vm.constants.ExecutionLevel;
import som.vm.constants.ReflectiveOp;
import som.vmobjects.SInvokable;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SObject;
import som.vmobjects.SReflectiveObject;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;

public abstract class MateAbstractSemanticNodes {

  public static abstract class MateEnvironmentSemanticCheckNode extends Node {

    private final ReflectiveOp reflectiveOperation;

    protected MateEnvironmentSemanticCheckNode(ReflectiveOp operation) {
      super();
      reflectiveOperation = operation;
    }

    public abstract SInvokable executeGeneric(VirtualFrame frame);

    
    @Specialization(guards = {"getEnvironment(frame)!=null", 
                  "getEnvironment(frame)==environment"})
    public SInvokable doSemanticsInFrame(final VirtualFrame frame,
        @Cached("getEnvironment(frame)") final SMateEnvironment environment,
        @Cached("methodImplementingOperationOn(environment)") final SInvokable reflectiveMethod) {
        return reflectiveMethod;
    }
    
    @Specialization(guards = "getEnvironment(frame)==null")
    public SInvokable doNoSemanticsInFrame(final VirtualFrame frame) {
      throw new MateSemanticsException();
    }
    
    public static SMateEnvironment getEnvironment(VirtualFrame frame){
      return SArguments.getEnvironment(frame);
    }
    
    public SInvokable methodImplementingOperationOn(final SMateEnvironment environment){
      return environment.methodImplementing(this.reflectiveOperation);
    }
  }

  public static abstract class MateObjectSemanticCheckNode extends Node {

    protected final ReflectiveOp reflectiveOperation;

    protected MateObjectSemanticCheckNode(ReflectiveOp operation) {
      super();
      reflectiveOperation = operation;
    }

    public abstract SInvokable executeGeneric(VirtualFrame frame,
        Object receiver);

    @Specialization(guards = "receiver.getEnvironment() == null")
    public SInvokable doStandardSOMBehavior(final VirtualFrame frame,
        final SReflectiveObject receiver) {
      return this.doSObject(frame, receiver);
    }

        
    @Specialization(guards = {"cachedEnvironment == receiver.getEnvironment()" , "method != null"})
    public SInvokable doSReflectiveObject(
        final VirtualFrame frame,
        final SReflectiveObject receiver,
        @Cached("receiver.getEnvironment()") final SMateEnvironment cachedEnvironment,
        @Cached("environmentReflectiveMethod(cachedEnvironment, reflectiveOperation)") final SInvokable method) {
      return method;
    }

    @Specialization(guards = "cachedEnvironment == receiver.getEnvironment()")
    public SInvokable doMetaObjectWithoutOperation(
        final VirtualFrame frame,
        final SReflectiveObject receiver,
        @Cached("receiver.getEnvironment()") final SMateEnvironment cachedEnvironment) {
        throw new MateSemanticsException();
    }
    
    @Specialization
    public SInvokable doSObject(final VirtualFrame frame, final Object receiver) {
      throw new MateSemanticsException();
    }

    protected static SInvokable environmentReflectiveMethod(
        SMateEnvironment environment, ReflectiveOp operation) {
      try{
        return environment.methodImplementing(operation);
      } catch (MateSemanticsException e){
        return null;
      }
    }
  }

  public static abstract class MateSemanticCheckNode extends Node {

    @Child MateEnvironmentSemanticCheckNode environment;
    @Child MateObjectSemanticCheckNode      object;

    public abstract SInvokable execute(final VirtualFrame frame,
        Object[] arguments);

    protected Object doMateDispatchNode(final VirtualFrame frame,
        final SInvokable environment, final SReflectiveObject receiver) {
      throw new MateSemanticsException();
    }

    public MateSemanticCheckNode(MateEnvironmentSemanticCheckNode env,
        MateObjectSemanticCheckNode obj) {
      super();
      environment = env;
      object = obj;
    }

    public static MateSemanticCheckNode createForFullCheck(
        SourceSection source, ReflectiveOp operation) {
      return MateSemanticCheckNodeGen.create(
          MateEnvironmentSemanticCheckNodeGen.create(operation),
          MateObjectSemanticCheckNodeGen.create(operation));
    }

    @Specialization(guards = "!executeBase(frame)")
    protected SInvokable executeSOM(final VirtualFrame frame, Object[] arguments) {
      throw new MateSemanticsException();
    }

    @Specialization(guards = "executeBase(frame)")
    protected SInvokable executeSemanticChecks(final VirtualFrame frame,
        Object[] arguments) {
      if (arguments[0] instanceof SObject){
        try{
          return environment.executeGeneric(frame);
        } catch (MateSemanticsException e) {
          return object.executeGeneric(frame, arguments[0]);
        }
      }
      throw new MateSemanticsException(); 
    }

    public MateSemanticCheckNode(final SourceSection source,
        ReflectiveOp operation) {
      super(source);
      environment = MateEnvironmentSemanticCheckNodeGen.create(operation);
      object = MateObjectSemanticCheckNodeGen.create(operation);
    }

    public static boolean executeBase(VirtualFrame frame) {
      return SArguments.getExecutionLevel(frame) == ExecutionLevel.Base;
    }
  }
}
