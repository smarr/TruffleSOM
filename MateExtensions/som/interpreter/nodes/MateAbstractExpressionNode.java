package som.interpreter.nodes;

import som.interpreter.nodes.MateAbstractExpressionNodeGen.MateFieldNodeGen;
import som.interpreter.nodes.MateAbstractExpressionNodeGen.MateMessageSendNodeGen;
import som.interpreter.nodes.MateAbstractReceiverNode.MateReceiverExpressionNode;
import som.interpreter.nodes.MateAbstractReflectiveDispatch.MateDispatchFieldAccess;
import som.interpreter.nodes.MateAbstractReflectiveDispatch.MateDispatchMessageLookup;
import som.interpreter.nodes.MateAbstractReflectiveDispatchFactory.MateDispatchFieldAccessNodeGen;
import som.interpreter.nodes.MateAbstractReflectiveDispatchFactory.MateDispatchMessageLookupNodeGen;
import som.interpreter.nodes.MateAbstractSemanticCheckNode.MateEnvironmentSemanticCheckNode;
import som.interpreter.nodes.MateAbstractSemanticCheckNode.MateObjectSemanticCheckNode;
import som.interpreter.nodes.MessageSendNode.AbstractMessageSendNode;
import som.vm.MateUniverse;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SObject;
import som.vmobjects.SReflectiveObject;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.ShortCircuit;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

@NodeChildren({
  @NodeChild(value = "receiver", type = MateAbstractReceiverNode.class),
  @NodeChild(value = "environment", type = MateEnvironmentSemanticCheckNode.class),
  @NodeChild(value = "object", type = MateObjectSemanticCheckNode.class, executeWith="receiver")
})
public abstract class MateAbstractExpressionNode extends ExpressionNode{
  
  protected Object doMateDispatchNode(VirtualFrame frame, SMateEnvironment environment, SObject receiver){return null;}
  protected Object doBaseSOMNode(VirtualFrame frame){return null;}
    
  public MateAbstractExpressionNode(Node node){
    super(node.getSourceSection());
  }
  
  public static MateAbstractExpressionNode createForNode(ExpressionWithReceiverNode node){
    if (node instanceof FieldNode){
      return MateFieldNodeGen.create((FieldNode)node,
          new MateReceiverExpressionNode(node), 
          MateEnvironmentSemanticCheckNode.create(), 
          MateObjectSemanticCheckNode.create());
    }
    return MateMessageSendNodeGen.create(node,    
        new MateReceiverExpressionNode(node), 
        MateEnvironmentSemanticCheckNode.create(), 
        MateObjectSemanticCheckNode.create());
  }
  
  /*public static MateAbstractNode createForNode(AbstractWriteFieldNode node){
    return MateFieldWriteNodeGen.create(node,
                              new MateReceiverNode(), 
                              MateEnvironmentSemanticCheckNode.create(), 
                              MateObjectSemanticCheckNode.create());
  }*/
    
  @ShortCircuit("environment")
  boolean needsContextSemanticsCheck(Object receiver) {
    return !(MateUniverse.current().executingMeta()) && (receiver instanceof SReflectiveObject);
  }
  
  @ShortCircuit("object")
  boolean needsObjectSemanticsCheck(Object receiver, boolean needContextSemanticsCheck, Object contextSemantics) {
    return needContextSemanticsCheck && (contextSemantics == null);
  }
  
  @Specialization(guards="!executingBaseLevel")
  public Object doPrimitiveObject(VirtualFrame frame,
                                    Object receiver,
                                    boolean executingBaseLevel,
                                    Object contextSemantics, 
                                    boolean needObjectSemanticsCheck, 
                                    Object objectSemantics){
    return this.doBaseSOMNode(frame);
  }
  
  @Specialization(guards="executeBaseLevel(executingBaseLevel, contextSemantics, objectSemantics)")
  public Object doSOMNode(VirtualFrame frame,
                                    Object receiver,
                                    boolean executingBaseLevel,
                                    Object contextSemantics, 
                                    boolean needObjectSemanticsCheck, 
                                    Object objectSemantics){
    return this.doBaseSOMNode(frame);
  }
  
  @Specialization
  public Object doMateDispatchWithContextSemantics(VirtualFrame frame,
      SReflectiveObject receiver,
      boolean executingBaseLevel,
      SMateEnvironment contextSemantics, 
      boolean needObjectSemanticsCheck, 
      Object objectSemantics){
    return this.doMateDispatchNode(frame, contextSemantics, receiver);
  }
  
  @Specialization(guards="needObjectSemanticsCheck")
  public Object doMateDispatchWithObjectSemantics(VirtualFrame frame,
      SReflectiveObject receiver,
      boolean executingBaseLevel,
      Object contextSemantics, 
      boolean needObjectSemanticsCheck, 
      SMateEnvironment objectSemantics){
    return this.doMateDispatchNode(frame, objectSemantics, receiver);
  }
  
  
  protected static boolean executeBaseLevel(boolean executingBaseLevel, Object contextSemantics, Object objectSemantics){
    return (!executingBaseLevel) || (contextSemantics == null && objectSemantics == null);
  }
  
  public abstract static class MateMessageSendNode extends MateAbstractExpressionNode{
    @Child protected MateDispatchMessageLookup mateDispatch;
    
    public MateMessageSendNode(ExpressionWithReceiverNode node) {
      super(node);
      mateDispatch = MateDispatchMessageLookupNodeGen.create(node);
    }
    
    protected Object doMateDispatchNode(VirtualFrame frame, SMateEnvironment environment, SObject receiver){
      return this.mateDispatch.executeDispatch(frame, environment, receiver);
    }
    
    protected Object doBaseSOMNode(VirtualFrame frame){
      return this.mateDispatch.doWrappedNode(frame);
    }
    
    public AbstractMessageSendNode getSOMWrappedNode(){
      return this.mateDispatch.getSOMWrappedNode();
    }
  }
  
  public abstract static class MateFieldNode extends MateAbstractExpressionNode{
    @Child protected MateDispatchFieldAccess mateDispatch;
    
    public MateFieldNode(FieldNode node) {
      super(node);
      mateDispatch = MateDispatchFieldAccessNodeGen.create(node);
    }
    
    protected Object doMateDispatchNode(VirtualFrame frame, SMateEnvironment environment, SObject receiver){
      return this.mateDispatch.executeDispatch(frame, environment, receiver);
    }
    
    protected Object doBaseSOMNode(VirtualFrame frame){
      return this.mateDispatch.doWrappedNode(frame);
    }
  }
  
  /*public abstract static class MateMessageActivationNode extends MateMessageSendNode{
    public MateMessageSendNode(AbstractMessageSendNode node) {
      super(node);
    }
    
    public Object[] evaluateArguments(final VirtualFrame frame) {
      return ((PreevaluatedExpression)this.wrappedNode).evaluateArguments(frame);
    }
  }
  
  public abstract static class MateFieldAccessNode extends MateAbstractExpressionNode{
    public MateFieldAccessNode(FieldNode node) {
      super(node);
    }
  }*/  

}  
