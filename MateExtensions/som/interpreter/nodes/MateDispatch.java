package som.interpreter.nodes;

import som.interpreter.SArguments;
import som.interpreter.Types;
import som.interpreter.nodes.MateDispatchNodeGen.MateDispatchMessageSendNodeGen;
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
  
  public abstract Object executeDispatch(final VirtualFrame frame, SMateEnvironment environment);
  
  public Object doBaselevel(final VirtualFrame frame){
    return baseLevel.executeGeneric(frame);
  };
  
  @Specialization(guards = "cachedEnvironment==environment")
  public Object doMetaLevel(final VirtualFrame frame,  
      SMateEnvironment environment,
      @Cached("environment") SMateEnvironment cachedEnvironment) 
  { 
    SMethod metaDelegation = this.createDispatch(environment, baseLevel.reflectiveOperation());
    if (metaDelegation == null)
      return doBaselevel(frame);
    else
      return this.doMeta(frame, metaDelegation);
  }
  
  public Object doMeta(final VirtualFrame frame, SMethod metaDelegation){
    return metaDelegation.invoke(frame);
  }
  
  @Specialization
  public Object doBaseLevel(final VirtualFrame frame, SMateEnvironment environment){
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
  
  public SMethod createDispatch(SObject metaobject, ReflectiveOp operation){
    return (SMethod)((SMateEnvironment)metaobject).methodImplementing(operation);
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
  
    public Object doMeta(final VirtualFrame frame, SMethod metaDelegation) {
      //The MOP receives the class where the lookup must start (find: aSelector since: aClass)
      SInvokable method = (SInvokable)metaDelegation.invoke(
                               SArguments.rcvr(frame), 
                               (SSymbol)SArguments.arg(frame, 1),
                               ((SObject)SArguments.rcvr(frame)).getSOMClass();                           
                             );
      //SClass rcvrClass = Types.getClassOf(SArguments.rcvr(frame));
      //SInvokable method = rcvrClass.lookupInvokable((SSymbol)SArguments.arg(frame, 1));
      CallTarget callTarget;
      if (method != null) {
        callTarget = method.getCallTarget();
      } else {
        callTarget = null;
      }
      return true;
    }
  }
  
}
