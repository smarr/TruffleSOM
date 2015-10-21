package som.matenodes;

import som.matenodes.MateAbstractSemanticNodesFactory.MateEnvironmentSemanticCheckNodeGen;
import som.matenodes.MateAbstractSemanticNodesFactory.MateObjectSemanticCheckNodeGen;
import som.matenodes.MateAbstractSemanticNodesFactory.MateSemanticCheckNodeGen;
import som.vm.MateSemanticsException;
import som.vm.MateUniverse;
import som.vm.constants.ReflectiveOp;
import som.vmobjects.SInvokable;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SReflectiveObject;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
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

    @Specialization(guards = "semanticsFromSlot(frame) != null")
    public SInvokable doSemanticsInFrame(final VirtualFrame frame,
        @Cached("semanticsFromSlot(frame)") final FrameSlot slot) {
      try {
        SMateEnvironment env = (SMateEnvironment) frame.getObject(slot);
        if (env == null) {
          return this.doNoSemanticsInFrame(frame);
        }
        return env.methodImplementing(this.reflectiveOperation);
      } catch (FrameSlotTypeException e) {
        return null;
      }
    }

    @Specialization(guards = "semanticsFromSlot(frame) == null")
    public SInvokable doNoSemanticsInFrame(final VirtualFrame frame) {
      return null;
    }

    public static FrameSlot semanticsFromSlot(final VirtualFrame frame) {
      return semanticsFromDescriptor(frame.getFrameDescriptor());
      // return null;
    }

    // TODO: remove this boundary, and fix the way 'semantics' objects are
    // handled
    @TruffleBoundary
    public static FrameSlot semanticsFromDescriptor(final FrameDescriptor desc) {
      return desc.findFrameSlot("semantics");
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

    @Child
    MateEnvironmentSemanticCheckNode environment;
    @Child
    MateObjectSemanticCheckNode      object;

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

    @Specialization(guards = "!executeBase(arguments)")
    protected SInvokable executeSOM(final VirtualFrame frame, Object[] arguments) {
      throw new MateSemanticsException();
    }

    @Specialization(guards = "executeBase(arguments)")
    protected SInvokable executeSemanticChecks(final VirtualFrame frame,
        Object[] arguments) {
      SReflectiveObject receiver = (SReflectiveObject) arguments[0];
      SInvokable environmentOfContext = environment.executeGeneric(frame);
      if (environmentOfContext == null) {
        SInvokable environmentOfReceiver = object.executeGeneric(frame,
            receiver);
        if (environmentOfReceiver == null) {
          throw new MateSemanticsException();
        }
        return environmentOfReceiver;
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

    public static boolean executeBase(Object[] arguments) {
      return (arguments[0] instanceof SReflectiveObject)
          && !MateUniverse.current().executingMeta();
    }
  }
}
