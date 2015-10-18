package som.matenodes;

import som.interpreter.SArguments;
import som.interpreter.nodes.MateMethodActivationNode;
import som.vm.MateUniverse;
import som.vm.constants.Nil;
import som.vmobjects.SArray;
import som.vmobjects.SArray.ArrayType;
import som.vmobjects.SInvokable;
import som.vmobjects.SInvokable.SMethod;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SObject;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.impl.DefaultTruffleRuntime;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ValueProfile;

public abstract class MateAbstractReflectiveDispatch extends Node {

  public MateAbstractReflectiveDispatch(final SourceSection source) {
    super(source);
  }

  protected Object[] computeArgumentsForMetaDispatch(Object[] arguments) {
    return arguments;
  }

  public DirectCallNode createDispatch(final SInvokable metaMethod) {
    return MateUniverse.current().getTruffleRuntime()
        .createDirectCallNode(metaMethod.getCallTarget());
  }

  public abstract static class MateDispatchFieldAccessor extends
      MateAbstractReflectiveDispatch {

    public MateDispatchFieldAccessor(final SourceSection source) {
      super(source);
    }
  }

  public abstract static class MateAbstractStandardDispatch extends
      MateAbstractReflectiveDispatch {

    public MateAbstractStandardDispatch(SourceSection source) {
      super(source);
    }

    public abstract Object executeDispatch(final VirtualFrame frame,
        SInvokable method, Object[] arguments);

    @Specialization(guards = "cachedMethod==method")
    public Object doMateNode(final VirtualFrame frame, final SInvokable method,
        final Object[] arguments,
        @Cached("method") final SInvokable cachedMethod,
        @Cached("createDispatch(method)") final DirectCallNode reflectiveMethod) {
      MateUniverse.current().enterMetaExecutionLevel();
      Object value = reflectiveMethod.call(frame,
          this.computeArgumentsForMetaDispatch(arguments));
      MateUniverse.current().leaveMetaExecutionLevel();
      return value;
    }
  }

  public abstract static class MateDispatchFieldAccess extends
      MateAbstractStandardDispatch {

    public MateDispatchFieldAccess(SourceSection source) {
      super(source);
    }
  }

  public abstract static class MateDispatchMessageLookup extends
      MateAbstractStandardDispatch {

    private final SSymbol    selector;
    @Child
    MateMethodActivationNode activationNode;

    public MateDispatchMessageLookup(SourceSection source, SSymbol sel) {
      super(source);
      selector = sel;
      activationNode = new MateMethodActivationNode();
    }

    @Override
    @Specialization(guards = "cachedMethod==method")
    public Object doMateNode(final VirtualFrame frame, final SInvokable method,
        final Object[] arguments,
        @Cached("method") final SInvokable cachedMethod,
        @Cached("createDispatch(method)") final DirectCallNode reflectiveMethod) {
      // The MOP receives the class where the lookup must start (find: aSelector
      // since: aClass)
      SObject receiver = (SObject) arguments[0];
      MateUniverse.current().enterMetaExecutionLevel();
      Object[] args = { receiver, this.getSelector(), receiver.getSOMClass() };
      SMethod actualMethod = (SMethod) reflectiveMethod.call(frame, args);
      MateUniverse.current().leaveMetaExecutionLevel();
      args[2] = SArguments.getArgumentsWithoutReceiver(arguments);
      return activationNode.doActivation(frame, actualMethod, args);
    }

    protected SSymbol getSelector() {
      return selector;
    }
  }

  public abstract static class MateActivationDispatch extends
      MateAbstractReflectiveDispatch {

    public MateActivationDispatch(SourceSection source) {
      super(source);
    }

    public abstract Object executeDispatch(final VirtualFrame frame,
        SInvokable method, SMethod methodToActivate, Object[] arguments);

    @Specialization(guards = "cachedMethod==method")
    public Object doMetaLevel(final VirtualFrame frame,
        final SInvokable method, final SMethod methodToActivate,
        final Object[] arguments,
        @Cached("method") final SInvokable cachedMethod,
        @Cached("createDispatch(method)") final DirectCallNode reflectiveMethod) {
      // The MOP receives the standard ST message Send stack (rcvr, method,
      // arguments) and return its own
      MateUniverse.current().enterMetaExecutionLevel();
      Object metacontext = reflectiveMethod.call(frame, arguments);
      MateUniverse.current().leaveMetaExecutionLevel();
      Object[] activationValue = this
          .gatherArrayFromSArray((SArray) metacontext);
      SMateEnvironment activationSemantics = (SMateEnvironment) activationValue[0];
      Object[] realArguments = this
          .gatherArrayFromSArray((SArray) activationValue[1]);
      if (activationSemantics != Nil.nilObject) {
        DefaultTruffleRuntime runtime = ((DefaultTruffleRuntime) MateUniverse
            .current().getTruffleRuntime());
        VirtualFrame customizedFrame = this.newFrameWithRedefinedSemanticsFor(
            realArguments, methodToActivate, activationSemantics, runtime);
        runtime.pushFrame(this.frameInstanceFor(customizedFrame,
            methodToActivate));
        try {
          return methodToActivate.getCallTarget().getRootNode()
              .execute(customizedFrame);
        } finally {
          runtime.popFrame();
        }
      }
      return methodToActivate.getCallTarget().call(realArguments);
    }

    private Object[] gatherArrayFromSArray(final SArray array) {
      if (array.getType() == ArrayType.PARTIAL_EMPTY) {
        return array
            .getPartiallyEmptyStorage(ValueProfile.createClassProfile())
            .getStorage();
      } else {
        return array.getObjectStorage(ValueProfile.createClassProfile());
      }
    }

    private FrameInstance frameInstanceFor(final VirtualFrame frame,
        final SMethod method) {
      return new FrameInstance() {

        @Override
        public Frame getFrame(final FrameAccess access, final boolean slowPath) {
          return frame;
        }

        @Override
        public boolean isVirtualFrame() {
          return false;
        }

        @Override
        public Node getCallNode() {
          return method.getCallTarget().getRootNode();
        }

        @Override
        public CallTarget getCallTarget() {
          return method.getCallTarget();
        }
      };
    }

    private VirtualFrame newFrameWithRedefinedSemanticsFor(
        final Object[] arguments, final SMethod methodToCall,
        final SMateEnvironment environment, final DefaultTruffleRuntime runtime) {
      VirtualFrame customizedFrame = runtime.createVirtualFrame(arguments,
          methodToCall.getCallTarget().getRootNode().getFrameDescriptor());
      FrameSlot slot = customizedFrame.getFrameDescriptor().addFrameSlot(
          "semantics", FrameSlotKind.Object);
      customizedFrame.setObject(slot, environment);
      return customizedFrame;
    }
  }
}
