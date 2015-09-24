package som.interpreter.nodes;

import som.interpreter.SArguments;
import som.interpreter.nodes.MateAbstractNodeGen.MateExpressionNodeGen;
import som.interpreter.nodes.MateAbstractReflectiveDispatchNodeGen.MateDispatchFieldAccessNodeGen;
import som.interpreter.nodes.MateAbstractReflectiveDispatchNodeGen.MateDispatchFieldLayoutNodeGen;
import som.interpreter.nodes.MateAbstractReflectiveDispatchNodeGen.MateDispatchMessageSendNodeGen;
import som.interpreter.nodes.MateAbstractSemanticCheckNode.MateEnvironmentSemanticCheckNode;
import som.interpreter.nodes.MateAbstractSemanticCheckNode.MateObjectSemanticCheckNode;
import som.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import som.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;
import som.vm.MateUniverse;
import som.vm.constants.ReflectiveOp;
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
  @NodeChild(value = "receiver", type = MateReceiverNode.class),
  @NodeChild(value = "environment", type = MateEnvironmentSemanticCheckNode.class),
  @NodeChild(value = "object", type = MateObjectSemanticCheckNode.class, executeWith="receiver")
})
public abstract class MateAbstractNode extends ExpressionNode{
  
  @Child protected MateAbstractReflectiveDispatch mateDispatch;
    
  protected Object dodoSomNode(VirtualFrame frame){
    return ((ExpressionNode)this.getSOMWrappedNode()).executeGeneric(frame);
  }
   
  public Node getSOMWrappedNode(){return null;}
  
  public MateAbstractNode(Node node){
    super(node.getSourceSection());
  }
  
  public static MateAbstractNode createForNode(ExpressionNode node){
    return MateExpressionNodeGen.create(node,
                              new MateReceiverNode((ExpressionWithReceiverNode)node), 
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
  
  public static abstract class MateExpressionNode extends MateAbstractNode{
    public MateExpressionNode(ExpressionNode node) {
      super(node);
      if (node instanceof FieldNode){
        mateDispatch = MateDispatchFieldAccessNodeGen.create(this);
      } else {
        mateDispatch = MateDispatchMessageSendNodeGen.create(this);
      }
      wrappedNode = node;
    }

    @Child protected ExpressionNode wrappedNode;
    
    @Override
    public ReflectiveOp reflectiveOperation(){
      return wrappedNode.reflectiveOperation();
    }
    
    @Override
    public Node getSOMWrappedNode(){
      return wrappedNode;
    }
  }
  
  public static abstract class MateFieldReadNode extends MateAbstractNode{
    public MateFieldReadNode(AbstractReadFieldNode node) {
      super(node);
      mateDispatch = MateDispatchFieldLayoutNodeGen.create(this);
      wrappedNode = node;
    }

    @Child protected AbstractReadFieldNode wrappedNode;
    
    public Object dodoSomNode(VirtualFrame frame){
      return this.wrappedNode.read((SObject)SArguments.rcvr(frame));
    }
    
    @Override
    public ReflectiveOp reflectiveOperation(){
      return wrappedNode.reflectiveOperation();
    }
    
    @Override
    public Node getSOMWrappedNode(){
      return wrappedNode;
    }
  }
  
  public static abstract class MateFieldWriteNode extends MateAbstractNode{
    public MateFieldWriteNode(AbstractWriteFieldNode node) {
      super(node);
      mateDispatch = MateDispatchFieldLayoutNodeGen.create(this);
      wrappedNode = node;
    }

    @Child protected AbstractWriteFieldNode wrappedNode;
    
    public Object dodoSomNode(VirtualFrame frame){
      return this.wrappedNode.write((SObject)SArguments.rcvr(frame), (SObject)SArguments.arg(frame, 1));
    }
    
    @Override
    public ReflectiveOp reflectiveOperation(){
      return wrappedNode.reflectiveOperation();
    }
    
    @Override
    public Node getSOMWrappedNode(){
      return wrappedNode;
    }
  }
}
