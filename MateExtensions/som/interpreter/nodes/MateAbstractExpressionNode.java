package som.interpreter.nodes;

import som.interpreter.SArguments;
import som.interpreter.nodes.MateAbstractExpressionNodeGen.MateExpressionNodeGen;
import som.interpreter.nodes.MateAbstractExpressionNodeGen.MateFieldReadNodeGen;
import som.interpreter.nodes.MateAbstractExpressionNodeGen.MateFieldWriteNodeGen;
import som.interpreter.nodes.MateAbstractReceiverNode.MateReceiverExpressionNode;
import som.interpreter.nodes.MateAbstractReceiverNode.MateReceiverNode;
import som.interpreter.nodes.MateAbstractReflectiveDispatchNodeGen.MateDispatchFieldAccessNodeGen;
import som.interpreter.nodes.MateAbstractReflectiveDispatchNodeGen.MateDispatchMessageSendNodeGen;
import som.interpreter.nodes.MateAbstractSemanticCheckNode.MateEnvironmentSemanticCheckNode;
import som.interpreter.nodes.MateAbstractSemanticCheckNode.MateObjectSemanticCheckNode;
import som.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import som.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;
import som.vm.MateUniverse;
import som.vm.constants.ReflectiveOp;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SObject;

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
public abstract class MateAbstractExpressionNode extends Node{
  @Child protected MateAbstractReflectiveDispatch mateDispatch;
    
  /*
   * This nodes are not instantiated. 
   * I had to define s concrete implementation of the methods below because of some problem 
   * with the annotation processor
   */
  protected Object dodoSomNode(VirtualFrame frame){return null;}
  protected ReflectiveOp reflectiveOperation(){return null;}
  
  public abstract Object executeGeneric(VirtualFrame frame);
    
  public MateAbstractExpressionNode(Node node){
    super(node.getSourceSection());
  }
  
  public static MateAbstractExpressionNode createForNode(ExpressionWithReceiverNode node){
    return MateExpressionNodeGen.create(node,
                              new MateReceiverExpressionNode(node), 
                              MateEnvironmentSemanticCheckNode.create(), 
                              MateObjectSemanticCheckNode.create());
  }
  
  public static MateAbstractExpressionNode createForNode(AbstractReadFieldNode node){
    return MateFieldReadNodeGen.create(node,
                              new MateReceiverNode(), 
                              MateEnvironmentSemanticCheckNode.create(), 
                              MateObjectSemanticCheckNode.create());
  }
  
  public static MateAbstractExpressionNode createForNode(AbstractWriteFieldNode node){
    return MateFieldWriteNodeGen.create(node,
                              new MateReceiverNode(), 
                              MateEnvironmentSemanticCheckNode.create(), 
                              MateObjectSemanticCheckNode.create());
  }
    
  @ShortCircuit("environment")
  boolean needsContextSemanticsCheck(Object receiver) {
    return !(MateUniverse.current().executingMeta());
  }
  
  @ShortCircuit("object")
  boolean needsObjectSemanticsCheck(Object receiver, boolean needContextSemanticsCheck, Object contextSemantics) {
    return needContextSemanticsCheck && (contextSemantics == null);
  }
  
  @Specialization(guards={"!executingBaseLevel"})
  public Object doSOMNode(VirtualFrame frame,
                                    Object receiver,
                                    boolean executingBaseLevel,
                                    Object contextSemantics, 
                                    boolean needObjectSemanticsCheck, 
                                    Object objectSemantics){
    return this.dodoSomNode(frame);
  }
  
  @Specialization(guards={"contextSemantics == null", "objectSemantics == null"})
  public Object doSOMNode2(VirtualFrame frame,
                                    Object receiver,
                                    boolean executingBaseLevel,
                                    Object contextSemantics, 
                                    boolean needObjectSemanticsCheck, 
                                    Object objectSemantics){
    return this.dodoSomNode(frame);
  }
  
  @Specialization(guards="!needObjectSemanticsCheck")
  public Object doMateNodeBecauseOfContextSemantics(VirtualFrame frame, 
                                    Object receiver,
                                    boolean executingBaseLevel,
                                    SMateEnvironment contextSemantics, 
                                    boolean needObjectSemanticsCheck, 
                                    Object objectSemantics){
    return doMateDispatch(frame, contextSemantics);
    
  }
  
  @Specialization(guards="needObjectSemanticsCheck")
  public Object doMateNodeBecauseOfObjectSemantics(VirtualFrame frame, 
                                    Object receiver,
                                    boolean executingBaseLevel,
                                    Object contextSemantics, 
                                    boolean needObjectSemanticsCheck, 
                                    SMateEnvironment objectSemantics){
    return doMateDispatch(frame, objectSemantics);
  }
  
  public Object doMateDispatch(VirtualFrame frame, SMateEnvironment semantics){
    Object value = mateDispatch.executeDispatch(frame, this.reflectiveOperation(), semantics);
    if (value == null){
      return dodoSomNode(frame);
    }
    return value;
  }
  
  
  
  public static abstract class MateExpressionNode extends MateAbstractExpressionNode{
    @Child protected ExpressionNode wrappedNode;
    
    public MateExpressionNode(ExpressionNode node) {
      super(node);
      if (node instanceof FieldNode){
        mateDispatch = MateDispatchFieldAccessNodeGen.create(this);
      } else {
        mateDispatch = MateDispatchMessageSendNodeGen.create(this);
      }
      wrappedNode = node;
    }

    @Override
    protected Object dodoSomNode(VirtualFrame frame){
      return this.wrappedNode.executeGeneric(frame);
    }
    
    @Override
    public ReflectiveOp reflectiveOperation(){
      return this.wrappedNode.reflectiveOperation();
    }
  }
  
  public static abstract class MateFieldWriteNode extends MateAbstractExpressionNode{
    public MateFieldWriteNode(AbstractWriteFieldNode node) {
      super(node);
      wrappedNode = node;
    }

    @Child protected AbstractWriteFieldNode wrappedNode;
    
    @Override
    public Object dodoSomNode(VirtualFrame frame){
      return this.wrappedNode.write((SObject)SArguments.rcvr(frame), (SObject)SArguments.arg(frame, 1));
    }
    
    @Override
    public ReflectiveOp reflectiveOperation(){
      return this.wrappedNode.reflectiveOperation();
    }
  }
  
  public static abstract class MateFieldReadNode extends MateAbstractExpressionNode{
    public MateFieldReadNode(AbstractReadFieldNode node) {
      super(node);
      wrappedNode = node;
    }

    @Child protected AbstractReadFieldNode wrappedNode;
    
    @Override
    public Object dodoSomNode(VirtualFrame frame){
      return this.wrappedNode.read((SObject)SArguments.rcvr(frame));
    }
    
    @Override
    public ReflectiveOp reflectiveOperation(){
      return this.wrappedNode.reflectiveOperation();
    }
  }
}  
