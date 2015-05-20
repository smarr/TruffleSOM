package som.interpreter.nodes;

import som.interpreter.SArguments;
import som.interpreter.nodes.MateDispatchNodeGen.MatePreEvaluatedDispatchNodeGen;
import som.interpreter.nodes.MessageSendNode.AbstractMessageSendNode;
import som.vm.constants.ReflectiveOp;
import som.vmobjects.SInvokable;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SReflectiveObject;
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
  
  public abstract Object executeDispatch(final VirtualFrame frame, boolean reimplementedInMOP, Object[] arguments);
  
  @Specialization(guards = "reimplementedInMOP")
  public Object doMetaLevel(final VirtualFrame frame,  
      final boolean reimplementedInMOP,
      Object[] arguments,
      @Cached("getMetaobjectFrom(arguments)") final SObject metaobject,
      @Cached("createDispatch(metaobject, baseLevel.reflectiveOperation())") final SMethod metaDelegation) 
  { 
    return metaDelegation.invoke(frame);
  }
  
  @Specialization(guards = "!reimplementedInMOP")
  public Object doBaseLevel(final VirtualFrame frame, 
      final boolean reimplementedInMOP,
      Object[] arguments) {
    return ((ExpressionNode) baseLevel).executeGeneric(frame);
    //return ((ExpressionNode) baseLevel).doPreEvaluated(frame,arguments);
  }
  
  public Object[] evaluateArguments(VirtualFrame frame){
    Object[] receiver = new Object[1];
    receiver[0] = SArguments.rcvr(frame);
    return receiver;
  }
  
  public static SObject getMetaobjectFrom(Object[] arguments){
    return ((SReflectiveObject)arguments[0]).getEnvironment();
  }
  
  public static SMethod createDispatch(SObject metaobject, ReflectiveOp operation){
    return (SMethod)((SMateEnvironment)metaobject).methodImplementing(operation);
  }
  
  public ReflectiveOp reflectiveOperation(){
    return this.baseLevel.reflectiveOperation();
  }
  
  public static abstract class MatePreEvaluatedDispatch extends MateDispatch {
        
    public MatePreEvaluatedDispatch(final PreevaluatedExpression node) {
      super((ExpressionNode)node);
    }
    
    public static MatePreEvaluatedDispatch create(final PreevaluatedExpression node) {
      return MatePreEvaluatedDispatchNodeGen.create(node);
    }
    
    public Object[] evaluateArguments(VirtualFrame frame){
      return ((PreevaluatedExpression)this.baseLevel).evaluateArguments(frame);
    }
    
    @Specialization(guards = "!reimplementedInMOP")
    public Object doBaseLevel(final VirtualFrame frame, 
        final boolean reimplementedInMOP,
        Object[] arguments) {
      return ((PreevaluatedExpression) baseLevel).doPreEvaluated(frame,arguments);
    }
  }
}
