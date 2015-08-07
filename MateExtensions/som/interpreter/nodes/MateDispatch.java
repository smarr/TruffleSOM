package som.interpreter.nodes;

import som.interpreter.SArguments;
import som.interpreter.Types;
import som.interpreter.nodes.MateDispatchNodeGen.MateDispatchMessageSendNodeGen;
import som.interpreter.nodes.MessageSendNode.AbstractMessageSendNode;
import som.vm.MateUniverse;
import som.vm.Universe;
import som.vm.constants.ReflectiveOp;
import som.vmobjects.SClass;
import som.vmobjects.SInvokable;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SInvokable.SMethod;
import som.vmobjects.SObject;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;


public abstract class MateDispatch extends Node {
  @CompilationFinal protected final ExpressionNode baseLevel;
  
  public MateDispatch(final ExpressionNode node){
    this.baseLevel = node;
  }
  
  public static MateDispatch create(final ExpressionNode node) {
    return MateDispatchNodeGen.create(node);
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
      @Cached("environment") SMateEnvironment cachedEnvironment) 
  { 
    SMethod[] metaDelegation = this.createDispatch(environment, baseLevel.reflectiveOperation());
    if (metaDelegation == null)
      return doBaselevel(frame, arguments);
    else {
      MateUniverse.current().enterMetaExecutionLevel();
      Object value = this.doMeta(frame, arguments, metaDelegation);
      MateUniverse.current().leaveMetaExecutionLevel();
      return value;
    }
  }
  
  @Specialization
  public Object doBaseLevel(final VirtualFrame frame, Object[] arguments, SMateEnvironment environment){
    return baseLevel.executeGeneric(frame);
  }
  
  public Object doMeta(final VirtualFrame frame, Object[] arguments, SMethod[] metaDelegation){
    return metaDelegation[0].invoke(frame);
  }
  
  public Object doBaselevel(final VirtualFrame frame, Object[] arguments){
    return baseLevel.executeGeneric(frame);
  }
  
  /*public Object[] evaluateArguments(VirtualFrame frame){
    Object[] receiver = new Object[1];
    receiver[0] = SArguments.rcvr(frame);
    return receiver;
  }
  
  public static SObject getMetaobjectFrom(Object[] arguments){
    return ((SReflectiveObject)arguments[0]).getEnvironment();
  }*/
  
  public SMethod[] createDispatch(SObject metaobject, ReflectiveOp operation){
    return ((SMateEnvironment)metaobject).methodsImplementing(operation);
  }
  
 /* public static abstract class MatePreEvaluatedDispatch extends MateDispatch {
        
    public MatePreEvaluatedDispatch(final PreevaluatedExpression node) {
      super((ExpressionNode)node);
    }
    
    public static MatePreEvaluatedDispatch create(final PreevaluatedExpression node) {
      return MatePreEvaluatedDispatchNodeGen.create(node);
    }
    
    public Object[] evaluateArguments(VirtualFrame frame){
      return ((PreevaluatedExpression)this.baseLevel).evaluateArguments(frame);
    }
    
    public Object executeGeneric(final VirtualFrame frame, SMateEnvironment environment){
      return ((PreevaluatedExpression) baseLevel).doPreEvaluated(frame,this.evaluateArguments(frame));
    }
  }*/
  
  public abstract static class MateDispatchMessageSend extends MateDispatch {
  
    public MateDispatchMessageSend(ExpressionNode node) {
      super(node);
      // TODO Auto-generated constructor stub
    }
    
    public static MateDispatchMessageSend create(final ExpressionNode node) {
      return MateDispatchMessageSendNodeGen.create(node);
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
        Object context = metaDelegation[1].invoke(arguments[0], method.getSignature(), SArguments.getArgumentsWithoutReceiver(arguments));
        MateUniverse.current().leaveMetaExecutionLevel();
        return callTarget.call(frame, context);
      } else {
        return callTarget.call(frame, arguments);
      }
    }
    
    @Override
    public Object[] evaluateArguments(final VirtualFrame frame) {
      return ((PreevaluatedExpression)this.baseLevel).evaluateArguments(frame);
   }
  }
}
