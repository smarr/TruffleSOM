package som.interpreter.nodes;

import som.interpreter.SArguments;
import som.interpreter.nodes.MateAbstractExpressionNodeGen.MateAbstractNode;
import som.interpreter.nodes.MateAbstractExpressionNodeGen.MateAbstractNode;
import som.interpreter.nodes.MateAbstractExpressionNodeGen.MateAbstractNode;
import som.interpreter.nodes.MateAbstractReceiverNode.MateReceiverExpressionNode;
import som.interpreter.nodes.MateAbstractReceiverNode.MateReceiverNode;
import som.interpreter.nodes.MateAbstractReflectiveDispatchNodeGen.MateDispatchFieldAccessNodeGen;
import som.interpreter.nodes.MateAbstractReflectiveDispatchNodeGen.MateDispatchMessageSendNodeGen;
import som.interpreter.nodes.MateAbstractSemanticCheckNode.MateEnvironmentSemanticCheckNode;
import som.interpreter.nodes.MateAbstractSemanticCheckNode.MateObjectSemanticCheckNode;
import som.interpreter.nodes.MessageSendNode.AbstractMessageSendNode;
import som.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import som.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;
import som.vm.MateUniverse;
import som.vm.Universe;
import som.vm.constants.Nil;
import som.vm.constants.ReflectiveOp;
import som.vmobjects.SArray;
import som.vmobjects.SInvokable;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SObject;
import som.vmobjects.SSymbol;
import som.vmobjects.SArray.ArrayType;
import som.vmobjects.SInvokable.SMethod;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.ShortCircuit;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.impl.DefaultTruffleRuntime;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.utilities.ValueProfile;

@NodeChildren({
  @NodeChild(value = "receiver", type = MateAbstractReceiverNode.class),
  @NodeChild(value = "environment", type = MateEnvironmentSemanticCheckNode.class),
  @NodeChild(value = "object", type = MateObjectSemanticCheckNode.class, executeWith="receiver")
})
public abstract class MateAbstractExpressionNode extends ExpressionNode{
  @Child protected MateAbstractReflectiveDispatch mateDispatch;
  @Child protected ExpressionWithReceiverNode wrappedNode;  
  
  public abstract Object executeGeneric(VirtualFrame frame);
  public abstract Object doMateDispatch(VirtualFrame frame, SMateEnvironment semantics);
    
  public MateAbstractExpressionNode(Node node){
    super(node.getSourceSection());
    if (node instanceof FieldNode){
      mateDispatch = MateDispatchFieldAccessNodeGen.create(this);
    } else {
      mateDispatch = MateDispatchMessageSendNodeGen.create(this);
    }
  }
  
  public static MateAbstractExpressionNode createForNode(ExpressionWithReceiverNode node){
    return MateExpressionNodeGen.create(node,
                              new MateReceiverExpressionNode(node), 
                              MateEnvironmentSemanticCheckNode.create(), 
                              MateObjectSemanticCheckNode.create());
  }
  
  /*public static MateAbstractNode createForNode(AbstractReadFieldNode node){
    return MateFieldReadNodeGen.create(node,
                              new MateReceiverNode(), 
                              MateEnvironmentSemanticCheckNode.create(), 
                              MateObjectSemanticCheckNode.create());
  }
  
  public static MateAbstractNode createForNode(AbstractWriteFieldNode node){
    return MateFieldWriteNodeGen.create(node,
                              new MateReceiverNode(), 
                              MateEnvironmentSemanticCheckNode.create(), 
                              MateObjectSemanticCheckNode.create());
  }*/
    
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
  
  protected Object dodoSomNode(VirtualFrame frame){return this.wrappedNode.executeGeneric(frame);}
  public ReflectiveOp reflectiveOperation(){return this.wrappedNode.reflectiveOperation();}

  
  public abstract static class MateMessageSendNode extends MateAbstractExpressionNode{
    public MateMessageSendNode(AbstractMessageSendNode node) {
      super(node);
    }

    @Override
    public Object doMateDispatch(VirtualFrame frame, SMateEnvironment semantics){
      Calltarget[] methods = mateDispatch.executeDispatch(frame, this.reflectiveOperation(), semantics, this.wrappedNode);
      if (methods == null){
        return dodoSomNode(frame);
      }
      //Todo: Compute arguments;
      Object[] arguments = this.evaluateArguments(frame); 
      //The MOP receives the class where the lookup must start (find: aSelector since: aClass)
      MateUniverse.current().enterMetaExecutionLevel();
      SInvokable method = (SInvokable)methods[0].invoke(
                               arguments[0], 
                               ((AbstractMessageSendNode)this.wrappedNode).getSelector(),
                               ((SObject)arguments[0]).getSOMClass()                           
                             );
      RootCallTarget callTarget = method.getCallTarget();
      if (methods[1] != null) {
        //The MOP receives the standard ST message Send stack (rcvr, selector, arguments) and return its own
        Object metacontext = methods[1].invoke(arguments[0], method.getSignature(), SArguments.getArgumentsWithoutReceiver(arguments));
        Object[] realArguments;
        if (((SArray)metacontext).getType() == ArrayType.PARTIAL_EMPTY){
          realArguments = ((SArray)metacontext).getPartiallyEmptyStorage(ValueProfile.createClassProfile()).getStorage();
        } else {
          realArguments = ((SArray)metacontext).getObjectStorage(ValueProfile.createClassProfile());
        }
        SMateEnvironment activationSemantics = (SMateEnvironment) realArguments[0];
        if (((SArray)realArguments[1]).getType() == ArrayType.PARTIAL_EMPTY){
          realArguments = ((SArray)realArguments[1]).getPartiallyEmptyStorage(ValueProfile.createClassProfile()).getStorage();
        } else {
          realArguments = ((SArray)realArguments[1]).getObjectStorage(ValueProfile.createClassProfile());
        }
        if (activationSemantics != Nil.nilObject){
          DefaultTruffleRuntime runtime = ((DefaultTruffleRuntime) Universe.current().getTruffleRuntime());
          VirtualFrame customizedFrame = runtime.createVirtualFrame(realArguments, callTarget.getRootNode().getFrameDescriptor());
          FrameSlot slot = customizedFrame.getFrameDescriptor().addFrameSlot("semantics", FrameSlotKind.Object);
          customizedFrame.setObject(slot, activationSemantics);
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
    
    public Object[] evaluateArguments(final VirtualFrame frame) {
      return ((PreevaluatedExpression)this.wrappedNode).evaluateArguments(frame);
    }
  }
  
  public abstract static class MateFieldAccessNode extends MateAbstractExpressionNode{
  
    public MateFieldAccessNode(FieldNode node) {
      super(node);
    }

    @Override
    public Object doMeta(final VirtualFrame frame, SMateEnvironment semantics){
      Calltarget[] methods = mateDispatch.executeDispatch(frame, this.reflectiveOperation(), semantics);
      MateUniverse.current().enterMetaExecutionLevel();
      Object value = methods[0].call(frame);
      MateUniverse.current().leaveMetaExecutionLevel();
      return value;
    }
  }  

}  
