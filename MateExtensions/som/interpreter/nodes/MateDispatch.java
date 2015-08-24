package som.interpreter.nodes;

import som.interpreter.SArguments;
import som.interpreter.nodes.MateDispatchNodeGen.MateDispatchMessageSendNodeGen;
import som.interpreter.nodes.MessageSendNode.AbstractMessageSendNode;
import som.interpreter.nodes.MessageSendNode.AbstractUninitializedMessageSendNode;
import som.vm.MateUniverse;
import som.vm.constants.ReflectiveOp;
import som.vmobjects.SArray;
import som.vmobjects.SInvokable;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SArray.ArrayType;
import som.vmobjects.SInvokable.SMethod;
import som.vmobjects.SObject;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.utilities.ValueProfile;


public abstract class MateDispatch extends Node {
  @CompilationFinal protected final ExpressionNode baseLevel;
  
  public MateDispatch(final ExpressionNode node){
    this.baseLevel = node;
  }
  
  public static MateDispatch create(final ExpressionNode node) {
    return MateDispatchNodeGen.create(node);
  }
  
  public ExpressionNode getBaseLevel(){
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
      @Cached("createDispatch(environment, baseLevel.reflectiveOperation())") SMethod[] reflectiveMethods) 
  { 
    if (reflectiveMethods == null)
      return doBase(frame, arguments);
    else {
      MateUniverse.current().enterMetaExecutionLevel();
      Object value = this.doMeta(frame, arguments, reflectiveMethods);
      MateUniverse.current().leaveMetaExecutionLevel();
      return value;
    }
  }
  
  @Specialization
  public Object doBaseLevel(final VirtualFrame frame, Object[] arguments, SMateEnvironment environment){
    return this.doBase(frame, arguments);
  }
  
  public Object doMeta(final VirtualFrame frame, Object[] arguments, SMethod[] metaDelegation){
    return metaDelegation[0].invoke(frame);
  }
  
  public Object doBase(final VirtualFrame frame, Object[] arguments){
    return baseLevel.executeGeneric(frame);
  }
  
  public SMethod[] createDispatch(SObject metaobject, ReflectiveOp operation){
    return ((SMateEnvironment)metaobject).methodsImplementing(operation);
  }
  
  public abstract static class MateDispatchMessageSend extends MateDispatch {
  
    public MateDispatchMessageSend(ExpressionNode node) {
      super(node);
      // TODO Auto-generated constructor stub
    }
    
    public static MateDispatchMessageSend create(final ExpressionNode node) {
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
      SInvokable method = (SInvokable)metaDelegation[0].invoke(
                               arguments[0], 
                               ((AbstractMessageSendNode)this.baseLevel).getSelector(),
                               ((SObject)arguments[0]).getSOMClass()                           
                             );
      CallTarget callTarget = method.getCallTarget();
      if (metaDelegation[1] != null) {
        //The MOP receives the standard ST message Send stack (rcvr, selector, arguments) and return its own
        Object metacontext = metaDelegation[1].invoke(arguments[0], method.getSignature(), SArguments.getArgumentsWithoutReceiver(arguments));
        Object[] realArguments;
        if (((SArray)metacontext).getType() == ArrayType.PARTIAL_EMPTY){
          realArguments = ((SArray)metacontext).getPartiallyEmptyStorage(ValueProfile.createClassProfile()).getStorage();
        } else {
          realArguments = ((SArray)metacontext).getObjectStorage(ValueProfile.createClassProfile());
        }
        return callTarget.call(realArguments);
      } else {
        return callTarget.call(frame, arguments);
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
