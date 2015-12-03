package som.matenodes;

import som.interpreter.SArguments;
import som.interpreter.nodes.MateMethodActivationNode;
import som.vm.MateUniverse;
import som.vm.constants.ExecutionLevel;
import som.vm.constants.Nil;
import som.vmobjects.SArray;
import som.vmobjects.SInvokable;
import som.vmobjects.SInvokable.SMethod;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SObject;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;

public abstract class MateAbstractReflectiveDispatch extends Node {

  public MateAbstractReflectiveDispatch(final SourceSection source) {
    super(source);
  }

  protected Object[] computeArgumentsForMetaDispatch(VirtualFrame frame, Object[] arguments) {
    return SArguments.createSArguments(SArguments.getEnvironment(frame), ExecutionLevel.Meta, arguments);
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
      //MateUniverse.current().enterMetaExecutionLevel();
      Object value = reflectiveMethod.call(frame, this.computeArgumentsForMetaDispatch(frame, arguments));
      //MateUniverse.current().leaveMetaExecutionLevel();
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
    @Child MateMethodActivationNode activationNode;

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
      // The MOP receives the class where the lookup must start (find: aSelector since: aClass)
      SObject receiver = (SObject) arguments[0];
      Object[] args = { SArguments.getEnvironment(frame), ExecutionLevel.Meta, receiver, this.getSelector(), receiver.getSOMClass() };
      SMethod actualMethod = (SMethod) reflectiveMethod.call(frame, args);
      return activationNode.doActivation(frame, actualMethod, arguments);
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
      // arguments) and returns its own
      MateUniverse.current().enterMetaExecutionLevel();
      Object[] args = { SArguments.getEnvironment(frame), ExecutionLevel.Meta, (SObject) arguments[0], methodToActivate, SArguments.getArgumentsWithoutReceiver(arguments)};
      Object metacontext = reflectiveMethod.call(frame, args);
      MateUniverse.current().leaveMetaExecutionLevel();
      Object[] activationValue = ((SArray) metacontext).toJavaArray();
      SMateEnvironment activationSemantics = (SMateEnvironment) activationValue[0];
      Object[] realArguments = ((SArray)activationValue[1]).toJavaArray();
      if (activationSemantics == Nil.nilObject) {
        activationSemantics = null;
      }
      return methodToActivate.getCallTarget().call(SArguments.createSArguments(activationSemantics, ExecutionLevel.Base, realArguments));
    }
  }
}
