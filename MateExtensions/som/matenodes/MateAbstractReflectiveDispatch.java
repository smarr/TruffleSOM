package som.matenodes;

import som.vm.MateUniverse;
import som.vmobjects.SInvokable;
import som.vmobjects.SInvokable.SMethod;
import som.vmobjects.SObject;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;

public abstract class MateAbstractReflectiveDispatch extends Node {

  public MateAbstractReflectiveDispatch(final SourceSection source){
    super(source);
  }
  
  public abstract Object executeDispatch(final VirtualFrame frame, SInvokable method, Object[] arguments);

  @Specialization(guards = "cachedMethod==method")
  public Object doMateNode(final VirtualFrame frame,
      final SInvokable method,
      final Object[] arguments,
      @Cached("method") final SInvokable cachedMethod,
      @Cached("createDispatch(method)") final DirectCallNode reflectiveMethod)
  {
        MateUniverse.current().enterMetaExecutionLevel();
        Object value = reflectiveMethod.call(frame, this.computeArgumentsForMetaDispatch(arguments));
        MateUniverse.current().leaveMetaExecutionLevel();
        return value;
  }
  
  protected Object[] computeArgumentsForMetaDispatch(Object[] arguments){
    return arguments;
  }

  public DirectCallNode createDispatch(final SInvokable metaMethod){
    return MateUniverse.current().getTruffleRuntime().createDirectCallNode(metaMethod.getCallTarget());
  }

  public abstract static class MateDispatchFieldAccessor extends MateAbstractReflectiveDispatch {
    public MateDispatchFieldAccessor(final SourceSection source) {
      super(source);
    }
  }


  public abstract static class MateDispatchFieldAccess extends MateAbstractReflectiveDispatch {
    
    public MateDispatchFieldAccess(SourceSection source) {
      super(source);
    }
  }

  public abstract static class MateDispatchMessageLookup extends MateAbstractReflectiveDispatch {
    //@Child MateDispatchActivation activationDispatch;

    public MateDispatchMessageLookup(SourceSection source) {
      super(source);
      //activationDispatch = MateDispatchActivationNodeGen.create(node);
    }

    @Specialization(guards = "cachedMethod==method")
    public Object doMateNode(final VirtualFrame frame,
        final SInvokable method,
        final Object[] arguments,
        @Cached("method") final SInvokable cachedMethod,
        @Cached("createDispatch(method)") final DirectCallNode reflectiveMethod)
    {
        //The MOP receives the class where the lookup must start (find: aSelector since: aClass)
        SObject receiver = (SObject) arguments[0];
        MateUniverse.current().enterMetaExecutionLevel();
        Object[] args = {receiver, this.getSelector(), receiver.getSOMClass()};
        SMethod actualMethod = (SMethod)reflectiveMethod.call(frame, args);
        MateUniverse.current().leaveMetaExecutionLevel();
        //activationDispatch.executeDispatch(frame, actualMethod, receiver, actualMethod);
        return actualMethod.getCallTarget().call(arguments);
    }

    protected SSymbol getSelector(){
      return null;
    }
  }

  /*public abstract static class MateDispatchActivation extends MateAbstractReflectiveDispatch {

    public MateDispatchActivation(final ExpressionWithReceiverNode node) {
      super(node.getSourceSection());
    }

    public abstract Object executeDispatch(final VirtualFrame frame, SInvokable method, Object[] arguments, SMethod methodToCall);
    
    /*Todo: Optimize: Isn't the operation always fixes*/
    /*@Specialization(guards = "cachedMethod==method")
    public Object doMetaLevel(final VirtualFrame frame,
        final SInvokable method,
        final Object[] arguments,
        final SMethod methodToCall,
        @Cached("method") final SInvokable cachedMethod,
        @Cached("createDispatch(method)") final DirectCallNode reflectiveMethod)
    {
        //The MOP receives the standard ST message Send stack (rcvr, selector, arguments) and return its own
        Object[] args = {arguments[0], methodToCall.getSignature(), SArguments.getArgumentsWithoutReceiver(arguments)};
        MateUniverse.current().enterMetaExecutionLevel();
        Object metacontext = reflectiveMethod.call(frame,args);
        MateUniverse.current().leaveMetaExecutionLevel();
        Object[] activationValue = this.gatherArrayFromSArray((SArray)metacontext);
        SMateEnvironment activationSemantics = (SMateEnvironment)activationValue[0];
        Object[] realArguments = this.gatherArrayFromSArray((SArray)activationValue[1]);
        if (activationSemantics != Nil.nilObject){
          DefaultTruffleRuntime runtime = ((DefaultTruffleRuntime) MateUniverse.current().getTruffleRuntime());
          VirtualFrame customizedFrame = this.newFrameWithRedefinedSemanticsFor(realArguments, methodToCall, activationSemantics, runtime);
          runtime.pushFrame(this.frameInstanceFor(customizedFrame, methodToCall));
          try {
            return methodToCall.getCallTarget().getRootNode().execute(customizedFrame);
          } finally {
            runtime.popFrame();
          }
        }
        return methodToCall.getCallTarget().call(realArguments);
    }

    private Object[] gatherArrayFromSArray(final SArray array){
      if (array.getType() == ArrayType.PARTIAL_EMPTY){
        return array.getPartiallyEmptyStorage(ValueProfile.createClassProfile()).getStorage();
      } else {
        return array.getObjectStorage(ValueProfile.createClassProfile());
      }
    }

    private FrameInstance frameInstanceFor(final VirtualFrame frame, final SMethod method){
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
    
    private VirtualFrame newFrameWithRedefinedSemanticsFor(final Object[] arguments, final SMethod methodToCall, final SMateEnvironment environment, final DefaultTruffleRuntime runtime){
      VirtualFrame customizedFrame = runtime.createVirtualFrame(arguments, methodToCall.getCallTarget().getRootNode().getFrameDescriptor());
      FrameSlot slot = customizedFrame.getFrameDescriptor().addFrameSlot("semantics", FrameSlotKind.Object);
      customizedFrame.setObject(slot, environment);
      return customizedFrame;
    }
  }*/
}
