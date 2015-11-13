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
      return null;
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

    @Specialization(
        guards = "cachedEnvironment == receiver.getEnvironment() || cachedEnvironment == null")
    public SInvokable doSReflectiveObject(
        final VirtualFrame frame,
        final SReflectiveObject receiver,
        @Cached("receiver.getEnvironment()") final SMateEnvironment cachedEnvironment,
        @Cached("environmentReflectiveMethod(cachedEnvironment, reflectiveOperation)") final SInvokable method) {
      return method;
    }

    @Specialization
    public SInvokable doSObject(final VirtualFrame frame, final Object receiver) {
      return null;
    }

    protected static SInvokable environmentReflectiveMethod(
        SMateEnvironment environment, ReflectiveOp operation) {
      return environment.methodImplementing(operation);
    }
  }

  public static abstract class MateSemanticCheckNode extends Node {

    @Child MateEnvironmentSemanticCheckNode environment;
    @Child MateObjectSemanticCheckNode      object;

    public abstract SInvokable execute(final VirtualFrame frame,
        Object[] arguments);

    protected Object doMateDispatchNode(final VirtualFrame frame,
        final SInvokable environment, final SReflectiveObject receiver) {
      throw new RuntimeException();
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
      SInvokable environmentOfContext = environment.executeGeneric(frame);
      if (environmentOfContext == null) {
        try{
          SReflectiveObject receiver = (SReflectiveObject) arguments[0];
          SInvokable environmentOfReceiver = object.executeGeneric(frame,
              receiver);
          if (environmentOfReceiver == null) {
            throw new MateSemanticsException();
          }
          return environmentOfReceiver;
        } catch (ClassCastException e){
          throw new MateSemanticsException();
        }
      }
      return environmentOfContext;
    }

    protected Object doBaseSOMNode(final VirtualFrame frame) {
      return null;
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
