package som.interpreter.nodes;

import som.interpreter.SArguments;
import som.interpreter.nodes.MateDispatchNodeGen.MateDispatchFieldAccessNodeGen;
import som.interpreter.nodes.MateDispatchNodeGen.MateDispatchFieldReadLayoutNodeGen;
import som.interpreter.nodes.MateDispatchNodeGen.MateDispatchFieldWriteLayoutNodeGen;
import som.interpreter.nodes.MateDispatchNodeGen.MateDispatchMessageSendNodeGen;
import som.interpreter.nodes.MessageSendNode.AbstractMessageSendNode;
import som.interpreter.nodes.MessageSendNode.AbstractUninitializedMessageSendNode;
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
import som.interpreter.objectstorage.FieldAccessorNode;
import som.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import som.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.impl.DefaultTruffleRuntime;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.utilities.ValueProfile;

public abstract class MateDispatch extends Node {
  @CompilationFinal protected final Node baseLevel;
  
  public MateDispatch(final Node node){
    this.baseLevel = node;
  }
  
  public static MateDispatch create(final Node node) {
    return MateDispatchNodeGen.create(node);
  }
  
  public Node getBaseLevel(){
    return this.baseLevel;
  }
  
  public Object[] evaluateArguments(final VirtualFrame frame) {
    Object[] receiver = new Object[1];
    receiver[0] = SArguments.rcvr(frame);
    return receiver;
  }
   
  public abstract Object executeDispatch(final VirtualFrame frame, Object[] arguments, SMateEnvironment environment);
  
  @Specialization(guards = "cachedEnvironment==environment")
  public Object doMetaLevel(final VirtualFrame frame,  
      Object[] arguments,
      SMateEnvironment environment,
      @Cached("environment") SMateEnvironment cachedEnvironment,
      @Cached("createDispatch(environment, baseLevel)") SMethod[] reflectiveMethods) 
  { 
    if (reflectiveMethods == null)
      return doBase(frame, arguments);
    else {
      return this.doMeta(frame, arguments, reflectiveMethods);
    }
  }
  
  @Specialization
  public Object doBaseLevel(final VirtualFrame frame, Object[] arguments, SMateEnvironment environment){
    return this.doBase(frame, arguments);
  }
  
  public Object doMeta(final VirtualFrame frame, Object[] arguments, SMethod[] metaDelegation){
    MateUniverse.current().enterMetaExecutionLevel();
    Object value = metaDelegation[0].invoke(frame);
    MateUniverse.current().leaveMetaExecutionLevel();
    return value;
  }
  
  public Object doBase(final VirtualFrame frame, Object[] arguments){
    return ((ExpressionNode) baseLevel).executeGeneric(frame);
  }
  
  public SMethod[] createDispatch(SObject metaobject, Node node){
    ReflectiveOp operation = ((ExpressionNode) node).reflectiveOperation();
    return ((SMateEnvironment)metaobject).methodsImplementing(operation);
  }
  
  public abstract static class MateDispatchFieldLayout extends MateDispatch {
    public MateDispatchFieldLayout(Node node) {
      super(node);
    }
    
    public Object doMeta(final VirtualFrame frame, Object[] arguments, SMethod[] metaDelegation){
      MateUniverse.current().enterMetaExecutionLevel();
      Object value = metaDelegation[0].invoke(arguments);
      MateUniverse.current().leaveMetaExecutionLevel();
      return value;
    }
    
    @Override
    public SMethod[] createDispatch(SObject metaobject, Node node){
      ReflectiveOp operation = ((FieldAccessorNode) node).reflectiveOperation();
      return ((SMateEnvironment)metaobject).methodsImplementing(operation);
    }
  }
  
  public abstract static class MateDispatchFieldReadLayout extends MateDispatchFieldLayout {
    public MateDispatchFieldReadLayout(Node node) {
      super(node);
    }
    
    public static MateDispatch create(final Node node) {
      return MateDispatchFieldReadLayoutNodeGen.create(node);
    }
    
    @Override
    public Object doBase(final VirtualFrame frame, Object[] arguments){
      return ((AbstractReadFieldNode) baseLevel).read((SObject)arguments[0]);
    }
  }
  
  public abstract static class MateDispatchFieldWriteLayout extends MateDispatchFieldLayout {
    public MateDispatchFieldWriteLayout(Node node) {
      super(node);
    }
    
    public static MateDispatch create(final Node node) {
      return MateDispatchFieldWriteLayoutNodeGen.create(node);
    }
    
    @Override
    public Object doBase(final VirtualFrame frame, Object[] arguments){
      return ((AbstractWriteFieldNode) baseLevel).write((SObject)arguments[0], arguments[1]);
    }
  }
  
  public abstract static class MateDispatchFieldAccess extends MateDispatch {

    public MateDispatchFieldAccess(Node node) {
      super(node);
    }
    
    public static MateDispatch create(final Node node) {
      return MateDispatchFieldAccessNodeGen.create(node);
    }
    
    public Object doMeta(final VirtualFrame frame, Object[] arguments, SMethod[] metaDelegation){
      MateUniverse.current().enterMetaExecutionLevel();
      Object value = metaDelegation[0].invoke(frame);
      MateUniverse.current().leaveMetaExecutionLevel();
      return value;
    }
  }  
   
  public abstract static class MateDispatchMessageSend extends MateDispatch {
  
    public MateDispatchMessageSend(Node node) {
      super(node);
    }
    
    public static MateDispatchMessageSend create(final Node node) {
      if (node instanceof AbstractUninitializedMessageSendNode){
        if (((AbstractUninitializedMessageSendNode) node).getSelector().getString() == "value"){
          return MateDispatchMessageSendNodeGen.create(node);
        } else {
          return MateDispatchMessageSendNodeGen.create(node);
        }
      } else {
        return MateDispatchMessageSendNodeGen.create(node);
      }
    }
  
    @Override
    public Object doMeta(final VirtualFrame frame, Object[] arguments, SMethod[] metaDelegation) {
      //The MOP receives the class where the lookup must start (find: aSelector since: aClass)
      MateUniverse.current().enterMetaExecutionLevel();
      SInvokable method = (SInvokable)metaDelegation[0].invoke(
                               arguments[0], 
                               ((AbstractMessageSendNode)this.baseLevel).getSelector(),
                               ((SObject)arguments[0]).getSOMClass()                           
                             );
      RootCallTarget callTarget = method.getCallTarget();
      if (metaDelegation[1] != null) {
        //The MOP receives the standard ST message Send stack (rcvr, selector, arguments) and return its own
        Object metacontext = metaDelegation[1].invoke(arguments[0], method.getSignature(), SArguments.getArgumentsWithoutReceiver(arguments));
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
    
    @Override
    public Object[] evaluateArguments(final VirtualFrame frame) {
      return ((PreevaluatedExpression)this.baseLevel).evaluateArguments(frame);
    }
    
    public Object doBase(final VirtualFrame frame, Object[] arguments){
      return ((PreevaluatedExpression)baseLevel).doPreEvaluated(frame, arguments);
    }
  }
}
