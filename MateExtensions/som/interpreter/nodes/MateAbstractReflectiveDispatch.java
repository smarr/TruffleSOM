package som.interpreter.nodes;

import som.interpreter.SArguments;
import som.interpreter.nodes.MateAbstractExpressionNode.MateExpressionNode;
import som.interpreter.nodes.MessageSendNode.AbstractMessageSendNode;
import som.interpreter.objectstorage.FieldAccessorNode;
import som.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import som.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;
import som.vm.MateUniverse;
import som.vm.Universe;
import som.vm.constants.Nil;
import som.vm.constants.ReflectiveOp;
import som.vmobjects.SArray;
import som.vmobjects.SInvokable;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SArray.ArrayType;
import som.vmobjects.SInvokable.SMethod;
import som.vmobjects.SObject;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
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
       
  public SMethod[] createDispatch(SObject metaobject, ReflectiveOp operation){
    return ((SMateEnvironment)metaobject).methodsImplementing(operation);
  }
  
  public abstract static class MateDispatchFieldAccessor extends MateAbstractReflectiveDispatch {
    public MateDispatchFieldAccessor(FieldAccessorNode node) {
      super(node);
    }
    
    public abstract Object executeDispatch(final VirtualFrame frame, SMateEnvironment environment, Object[] arguments);
    public abstract Object doSomNode(final VirtualFrame frame, SMateEnvironment environment, Object[] arguments);
    
    @Specialization(guards = {"(cachedEnvironment==environment)","reflectiveMethod != null"})
    public Object doMateNode(final VirtualFrame frame,  
        SMateEnvironment environment,
        Object[] arguments,
        @Cached("environment") SMateEnvironment cachedEnvironment,
        @Cached("createDispatch(environment)") DirectCallNode reflectiveMethod) 
    { 
        MateUniverse.current().enterMetaExecutionLevel();
        Object value = reflectiveMethod.call(frame, arguments);
        MateUniverse.current().leaveMetaExecutionLevel();
        return value;
    }
  }
  
  public abstract static class MateDispatchFieldReadLayout extends MateDispatchFieldAccessor {
    @Child protected AbstractReadFieldNode wrappedNode;
    
    @Specialization()
    public Object doSomNode(final VirtualFrame frame, SMateEnvironment environment, Object[] arguments){ 
        this.wrappedNode.read((SObject)arguments[0]);
    }
    
    public MateDispatchFieldReadLayout(AbstractReadFieldNode node) {
      super(node);
      wrappedNode = node;
    }
  }
  
  public abstract static class MateDispatchFieldWriteLayout extends MateDispatchFieldAccessor {
    @Child protected AbstractWriteFieldNode wrappedNode;
    
    public MateDispatchFieldWriteLayout(AbstractWriteFieldNode node) {
      super(node);
      wrappedNode = node;
    }
    
    @Specialization()
    public Object doSomNode(final VirtualFrame frame, SMateEnvironment environment, Object[] arguments){ 
      this.wrappedNode.write((SObject)arguments[0], (SObject)arguments[1]);
    }  
  }
  
  public abstract static class MateDispatchFieldAccess extends MateAbstractReflectiveDispatch {

    public MateDispatchFieldAccess(Node node) {
      super(node);
    }
    
    public abstract Object executeDispatch(final VirtualFrame frame, ReflectiveOp operation, SMateEnvironment environment);
    
    @Specialization(guards = "(cachedEnvironment==environment)")
    public Object doMetaLevel(final VirtualFrame frame,  
        ReflectiveOp operation,
        SMateEnvironment environment,
        @Cached("environment") SMateEnvironment cachedEnvironment,
        @Cached("operation") ReflectiveOp cachedOperation,
        @Cached("createDispatch(environment, operation)") DirectCallNode reflectiveMethod) 
    { 
        MateUniverse.current().enterMetaExecutionLevel();
        Object value = reflectiveMethod.call(frame, null);
        MateUniverse.current().leaveMetaExecutionLevel();
        return value;
    }
  }  
   
  public abstract static class MateDispatchMessageLookup extends MateAbstractReflectiveDispatch {
    public final MateAbstractExpressionNode mateNode;
    
    public MateDispatchMessageSend(MateAbstractExpressionNode node) {
      super(node);
      mateNode = node;
    }
    
    private SSymbol getSelector(){
      return ((AbstractMessageSendNode)this.mateNode.wrappedNode).getSelector();
    }
    
    public abstract Object executeDispatch(final VirtualFrame frame, ReflectiveOp operation, SMateEnvironment environment);
    
    /*Todo: Optimize: Isn't the operation always fixes*/
    @Specialization(guards = "(cachedEnvironment==environment)")
    public Object doMetaLevel(final VirtualFrame frame,  
        ReflectiveOp operation,
        SMateEnvironment environment,
        @Cached("environment") SMateEnvironment cachedEnvironment,
        @Cached("operation") ReflectiveOp cachedOperation,
        @Cached("createDispatch(environment, operation)") DirectCallNode reflectiveMethod) 
    { 
      if (reflectiveMethod == null)
        return null;
      else {
        //Todo: Compute arguments;
        Object[] arguments = this.evaluateArguments(frame); 
        //The MOP receives the class where the lookup must start (find: aSelector since: aClass)
        MateUniverse.current().enterMetaExecutionLevel();
        Object[] args = {arguments[0], 
            this.getSelector(),
            ((SObject)arguments[0]).getSOMClass()};
        return reflectiveMethod.call(frame, args);
      }
    }
  }
  
  public abstract static class MateDispatchActivation extends MateAbstractReflectiveDispatch {
    public abstract Object executeDispatch(final VirtualFrame frame, ReflectiveOp operation, SMateEnvironment environment);
    
    /*Todo: Optimize: Isn't the operation always fixes*/
    @Specialization(guards = "(cachedEnvironment==environment)")
    public Object doMetaLevel(final VirtualFrame frame,  
        ReflectiveOp operation,
        SMateEnvironment environment,
        @Cached("environment") SMateEnvironment cachedEnvironment,
        @Cached("operation") ReflectiveOp cachedOperation,
        @Cached("createDispatch(environment, operation)") DirectCallNode reflectiveMethod) 
    { 
      if (reflectiveMethod == null)
        return null;
      else {
          //The MOP receives the standard ST message Send stack (rcvr, selector, arguments) and return its own
          Object[] arguments = this.evaluateArguments(frame);
          Object[] args = {arguments[0], method.getSignature(), SArguments.getArgumentsWithoutReceiver(arguments)};
          Object metacontext = reflectiveMethod.call(frame,args);
          Object[] realArguments;
          if (((SArray)metacontext).getType() == ArrayType.PARTIAL_EMPTY){
            realArguments = ((SArray)metacontext).getPartiallyEmptyStorage(ValueProfile.createClassProfile()).getStorage();
          } else {
            realArguments = ((SArray)metacontext).getObjectStorage(ValueProfile.createClassProfile());
          }
          SMateEnvironment semantics = (SMateEnvironment) realArguments[0];
          if (((SArray)realArguments[1]).getType() == ArrayType.PARTIAL_EMPTY){
            realArguments = ((SArray)realArguments[1]).getPartiallyEmptyStorage(ValueProfile.createClassProfile()).getStorage();
          } else {
            realArguments = ((SArray)realArguments[1]).getObjectStorage(ValueProfile.createClassProfile());
          }
          if (semantics != Nil.nilObject){
            DefaultTruffleRuntime runtime = ((DefaultTruffleRuntime) Universe.current().getTruffleRuntime());
            VirtualFrame customizedFrame = runtime.createVirtualFrame(realArguments, callTarget.getRootNode().getFrameDescriptor());
            FrameSlot slot = customizedFrame.getFrameDescriptor().addFrameSlot("semantics", FrameSlotKind.Object);
            customizedFrame.setObject(slot, semantics);
            FrameInstance frameInstance = new FrameInstance() {
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
            runtime.pushFrame(frameInstance);
            try {
              MateUniverse.current().leaveMetaExecutionLevel();
              return method.getCallTarget().getRootNode().execute(customizedFrame);
            } finally {
              runtime.popFrame();
            }
          }
          MateUniverse.current().leaveMetaExecutionLevel();
          return callTarget.call(realArguments);
        } else {
          MateUniverse.current().leaveMetaExecutionLevel();
          return callTarget.call(arguments);
        }
      }
    }
  }
}
