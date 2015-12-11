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

    @Specialization(guards = "getEnvironment(frame) != null")
    public SInvokable doSemanticsInFrame(final VirtualFrame frame,
        @Cached("getEnvironment(frame)") final SMateEnvironment environment) {
        if (environment == null) {
          return this.doNoSemanticsInFrame(frame);
        }
        return environment.methodImplementing(this.reflectiveOperation);
    }

    @Specialization(guards = "getEnvironment(frame) == null")
    public SInvokable doNoSemanticsInFrame(final VirtualFrame frame) {
      throw new MateSemanticsException();
    }
    
    public static SMateEnvironment getEnvironment(VirtualFrame frame){
      return SArguments.getEnvironment(frame);
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

    @Specialization(guards = "cachedEnvironment == receiver.getEnvironment()")
    public SInvokable doSReflectiveObject(
        final VirtualFrame frame,
        final SReflectiveObject receiver,
        @Cached("receiver.getEnvironment()") final SMateEnvironment cachedEnvironment,
        @Cached("environmentReflectiveMethod(cachedEnvironment, reflectiveOperation)") final SInvokable method) {
      /*Check if we can do better to avoid the execution of this if. One option is to do the specialization by hand*/
      if (method == null){
        throw new MateSemanticsException();
      }
      return method;
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
      try{
        return environment.executeGeneric(frame);
      } catch (MateSemanticsException e) {
        if (arguments[0] instanceof SReflectiveObject){
          return object.executeGeneric(frame, (SReflectiveObject) arguments[0]);
        } else {
          throw new MateSemanticsException();
        }
      }
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
