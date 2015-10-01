package som.interpreter.nodes;

import som.interpreter.SArguments;
import som.interpreter.nodes.MateAbstractReflectiveDispatchFactory.MateDispatchActivationNodeGen;
import som.interpreter.nodes.MessageSendNode.AbstractMessageSendNode;
import som.interpreter.objectstorage.FieldAccessorNode;
import som.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import som.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;
import som.vm.MateUniverse;
import som.vm.constants.Nil;
import som.vm.constants.ReflectiveOp;
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
import com.oracle.truffle.api.utilities.ValueProfile;

public abstract class MateAbstractReflectiveDispatch extends Node {
  
  public MateAbstractReflectiveDispatch(Node node){
    super(node.getSourceSection());
  }
  
  /*public Object[] evaluateArguments(final VirtualFrame frame) {
    Object[] receiver = new Object[1];
    receiver[0] = SArguments.rcvr(frame);
    return receiver;
  }*/
       
  public DirectCallNode createDispatch(SMateEnvironment metaobject){
    SInvokable target = metaobject.methodImplementing(this.getReflectiveOperation());
    if (target != null){
       return MateUniverse.current().getTruffleRuntime().createDirectCallNode(target.getCallTarget());
    }
    return null;
  }
  
  protected abstract ReflectiveOp getReflectiveOperation();
  
  public abstract static class MateDispatchFieldAccessor extends MateAbstractReflectiveDispatch {
    public MateDispatchFieldAccessor(FieldAccessorNode node) {
      super(node);
    }
    
    public abstract Object executeDispatch(final VirtualFrame frame, Object environment, Object[] arguments);
    public Object doWrappedNode(Object[] arguments){return null;}
     
    @Specialization(guards = "cachedEnvironment==environment")
    public Object doMateNode(final VirtualFrame frame,  
        SMateEnvironment environment,
        Object[] arguments,
        @Cached("environment") SMateEnvironment cachedEnvironment,
        @Cached("createDispatch(environment)") DirectCallNode reflectiveMethod) 
    { 
        if (reflectiveMethod != null) {
          MateUniverse.current().enterMetaExecutionLevel();
          Object value = reflectiveMethod.call(frame, arguments);
          MateUniverse.current().leaveMetaExecutionLevel();
          return value;
        }
        return doSomNode(frame, environment, arguments);
    }
    
    @Specialization(guards= "environment == null")
    public Object doSomNode(final VirtualFrame frame, Object environment, Object[] arguments){ 
        return this.doWrappedNode(arguments);
    }
    
    protected ReflectiveOp getReflectiveOperation(){return null;}
  }
  
  public abstract static class MateDispatchFieldReadLayout extends MateDispatchFieldAccessor {
    @Child protected AbstractReadFieldNode wrappedNode;
    
    public MateDispatchFieldReadLayout(AbstractReadFieldNode node) {
      super(node);
      wrappedNode = node;
    }
    
    protected ReflectiveOp getReflectiveOperation(){
      return this.wrappedNode.reflectiveOperation();
    }
    
    @Override
    public Object doWrappedNode(Object[] arguments){
      return this.wrappedNode.read((SObject)arguments[0]);
    }
  }
  
  public abstract static class MateDispatchFieldWriteLayout extends MateDispatchFieldAccessor {
    @Child protected AbstractWriteFieldNode wrappedNode;
    
    public MateDispatchFieldWriteLayout(AbstractWriteFieldNode node) {
      super(node);
      wrappedNode = node;
    }
    
    @Override
    public Object doWrappedNode(Object[] arguments){
      return this.wrappedNode.write((SObject)arguments[0], arguments[2]);
    }
    
    protected ReflectiveOp getReflectiveOperation(){
      return this.wrappedNode.reflectiveOperation();
    }  
  }
  
  public abstract static class MateDispatchFieldAccess extends MateAbstractReflectiveDispatch {
    @Child protected FieldNode wrappedNode;
    
    public MateDispatchFieldAccess(FieldNode node) {
      super(node);
      wrappedNode = node;
    }
    
    public abstract Object executeDispatch(final VirtualFrame frame, Object environment, Object receiver);
    
    @Specialization(guards = {"cachedEnvironment==environment"})
    public Object doMetaLevel(final VirtualFrame frame,  
        SMateEnvironment environment,
        SObject receiver,
        @Cached("environment") SMateEnvironment cachedEnvironment,
        @Cached("createDispatch(environment)") DirectCallNode reflectiveMethod) 
    { 
        if (reflectiveMethod != null){
          MateUniverse.current().enterMetaExecutionLevel();
          Object value = reflectiveMethod.call(frame, this.wrappedNode.argumentsForReceiver(frame, receiver));
          MateUniverse.current().leaveMetaExecutionLevel();
          return value;
        }
        return this.doWrappedNode(frame); 
    }
    
    @Specialization(guards= "environment == null")
    public Object doSomNode(final VirtualFrame frame, Object environment, SObject receiver){
      return this.doWrappedNode(frame);
    }
    
    public Object doWrappedNode(final VirtualFrame frame){
      return this.wrappedNode.executeGeneric(frame);
    }
    
    protected ReflectiveOp getReflectiveOperation(){
      return this.wrappedNode.reflectiveOperation();
    }  
  }  
  
  public abstract static class MateDispatchMessageLookup extends MateAbstractReflectiveDispatch {
    @Child protected ExpressionWithReceiverNode wrappedNode;
    @Child MateDispatchActivation activationDispatch;
    
    public MateDispatchMessageLookup(ExpressionWithReceiverNode node) {
      super(node);
      this.wrappedNode = node;
      activationDispatch = MateDispatchActivationNodeGen.create(node);
    }
    
    public abstract Object executeDispatch(final VirtualFrame frame, SMateEnvironment environment, Object receiver);
    
    @Specialization(guards= "environment == null")
    public Object doSomNode(final VirtualFrame frame, SMateEnvironment environment, SObject receiver){
      return this.doWrappedNode(frame);
    }
    
    @Specialization(guards = {"(cachedEnvironment==environment)"})
    public Object doMetaLevel(final VirtualFrame frame,  
        SMateEnvironment environment,
        SObject receiver,
        @Cached("environment") SMateEnvironment cachedEnvironment,
        @Cached("createDispatch(environment)") DirectCallNode reflectiveMethod) 
    { 
        if (reflectiveMethod != null){
          //The MOP receives the class where the lookup must start (find: aSelector since: aClass)
          MateUniverse.current().enterMetaExecutionLevel();
          Object[] args = {receiver, this.getSelector(), receiver.getSOMClass()};
          SMethod method = (SMethod)reflectiveMethod.call(frame, args);
          MateUniverse.current().leaveMetaExecutionLevel();
          return activationDispatch.executeDispatch(frame, environment, receiver, method);
        }
        return this.doWrappedNode(frame);
    }
    
    protected ReflectiveOp getReflectiveOperation(){
      return this.wrappedNode.reflectiveOperation();
    }
    
    public AbstractMessageSendNode getSOMWrappedNode(){
      return (AbstractMessageSendNode)this.wrappedNode;
    }
    
    protected SSymbol getSelector(){
      return ((AbstractMessageSendNode)this.wrappedNode).getSelector();
    }
    
    public Object doWrappedNode (final VirtualFrame frame){
      return this.wrappedNode.executeGeneric(frame);
    }
  }
  
  public abstract static class MateDispatchActivation extends MateAbstractReflectiveDispatch {
    protected final ExpressionWithReceiverNode wrappedNode;
    
    public MateDispatchActivation(ExpressionWithReceiverNode node) {
      super(node);
      wrappedNode = node; 
    }
    
    public abstract Object executeDispatch(final VirtualFrame frame, SMateEnvironment environment, Object receiver, SMethod methodToCall);
    
    protected ReflectiveOp getReflectiveOperation(){
      return ReflectiveOp.Activation;
    }  

    @Specialization(guards= "environment == null")
    public Object doSomNode(final VirtualFrame frame, SMateEnvironment environment, SObject receiver, SMethod methodToCall){
      return methodToCall.getCallTarget().call(((PreevaluatedExpression) wrappedNode).evaluateArguments(frame));
    }
    
    /*Todo: Optimize: Isn't the operation always fixes*/
    @Specialization(guards = "(cachedEnvironment==environment)")
    public Object doMetaLevel(final VirtualFrame frame,  
        SMateEnvironment environment,
        SObject receiver,
        SMethod methodToCall,
        @Cached("environment") SMateEnvironment cachedEnvironment,
        @Cached("createDispatch(environment)") DirectCallNode reflectiveMethod) 
    { 
      Object[] arguments = ((PreevaluatedExpression) wrappedNode).evaluateArguments(frame);
      if (reflectiveMethod != null){
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
      } else {
        return methodToCall.getCallTarget().call(arguments);
      }
    }
    
    private Object[] gatherArrayFromSArray(SArray array){
      if (array.getType() == ArrayType.PARTIAL_EMPTY){
        return array.getPartiallyEmptyStorage(ValueProfile.createClassProfile()).getStorage();
      } else {
        return array.getObjectStorage(ValueProfile.createClassProfile());
      }
    }
    
    private FrameInstance frameInstanceFor(VirtualFrame frame, SMethod method){
      return new FrameInstance() {
        public Frame getFrame(FrameAccess access, boolean slowPath) {
            return frame;
        }

        public boolean isVirtualFrame() {
            return false;
        }

        public Node getCallNode() {
            return method.getCallTarget().getRootNode();
        }

        public CallTarget getCallTarget() {
            return method.getCallTarget();
        }
      };
    }
    private VirtualFrame newFrameWithRedefinedSemanticsFor(Object[] arguments, SMethod methodToCall, SMateEnvironment environment, DefaultTruffleRuntime runtime){
      VirtualFrame customizedFrame = runtime.createVirtualFrame(arguments, methodToCall.getCallTarget().getRootNode().getFrameDescriptor());
      FrameSlot slot = customizedFrame.getFrameDescriptor().addFrameSlot("semantics", FrameSlotKind.Object);
      customizedFrame.setObject(slot, environment);
      return customizedFrame;
    }
  }
}
