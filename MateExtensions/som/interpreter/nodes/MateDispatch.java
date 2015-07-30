package som.interpreter.nodes;

import som.vm.constants.ReflectiveOp;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SInvokable.SMethod;
import som.vmobjects.SObject;

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
}
