package som.interpreter.nodes;

import som.interpreter.nodes.MateDispatchNodeGen.MatePreEvaluatedDispatchNodeGen;
import som.interpreter.nodes.MessageSendNode.AbstractMessageSendNode;

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
  
  @Specialization(guards = "!reimplementedInMOP")
  public Object doBaseLevel(final VirtualFrame frame, 
      final boolean reimplementedInMOP,
      Object[] arguments) {
    return ((ExpressionNode) baseLevel).executeGeneric(frame);
    //return ((ExpressionNode) baseLevel).doPreEvaluated(frame,arguments);
  }
  
  public Object[] evaluateArguments(VirtualFrame frame){
    return new Object[0];
  }
  
  @Specialization(contains = "doBaseLevel", guards = "reimplementedInMOP")
  public Object doMetaLevel(final VirtualFrame frame,  
      final boolean reimplementedInMOP,
      Object[] arguments) 
  { 
      //@Cached("createMetaDelegation()") final ExpressionNode cachedMetaDelegation) {
    //Object[] arguments = null;
    //return cachedMetaDelegation.executeGeneric(frame);
    return 1;
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
    
    @Specialization(guards = "reimplementedInMOP")
    public Object doBaseLevel(final VirtualFrame frame, 
        final boolean reimplementedInMOP,
        Object[] arguments) {
      return ((AbstractMessageSendNode) baseLevel).doPreEvaluated(frame,arguments);
    }
    
    @Specialization(contains = "doBaseLevel")
    public Object doMetaLevel(final VirtualFrame frame,  
        final boolean reimplementedInMOP,
        Object[] arguments) 
    {
        //@Cached("createMetaDelegation()") final ExpressionNode cachedMetaDelegation) {
      //Object[] arguments = null;
      //return cachedMetaDelegation.executeGeneric(frame);
      return 1;
    }
  }
}
