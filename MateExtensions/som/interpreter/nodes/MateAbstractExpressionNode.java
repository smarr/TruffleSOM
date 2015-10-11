package som.interpreter.nodes;

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
import com.oracle.truffle.api.source.SourceSection;

@NodeChildren({
  @NodeChild(value = "receiver", type = MateAbstractReceiverNode.class),
  @NodeChild(value = "environment", type = MateEnvironmentSemanticCheckNode.class),
  @NodeChild(value = "object", type = MateObjectSemanticCheckNode.class, executeWith="receiver")
})
public abstract class MateAbstractExpressionNode extends ExpressionNode {

  protected Object doMateDispatchNode(final VirtualFrame frame, final SMateEnvironment environment, final SObject receiver){return null;}
  protected Object doBaseSOMNode(final VirtualFrame frame){return null;}

  public MateAbstractExpressionNode(final SourceSection source){
    super(source);
  }

  /*public static MateAbstractNode createForNode(AbstractWriteFieldNode node){
    return MateFieldWriteNodeGen.create(node,
                              new MateReceiverNode(),
                              MateEnvironmentSemanticCheckNode.create(),
                              MateObjectSemanticCheckNode.create());
  }*/

  @ShortCircuit("environment")
  boolean needsContextSemanticsCheck(final Object receiver) {
    return !(MateUniverse.current().executingMeta()) && (receiver instanceof SReflectiveObject);
  }

  @ShortCircuit("object")
  boolean needsObjectSemanticsCheck(final Object receiver, final boolean needContextSemanticsCheck, final Object contextSemantics) {
    return needContextSemanticsCheck && (contextSemantics == null);
  }

  @Specialization(guards="!executingBaseLevel")
  public Object doPrimitiveObject(final VirtualFrame frame,
                                    final Object receiver,
                                    final boolean executingBaseLevel,
                                    final Object contextSemantics,
                                    final boolean needObjectSemanticsCheck,
                                    final Object objectSemantics){
    return this.doBaseSOMNode(frame);
  }

  @Specialization(guards="executeBaseLevel(executingBaseLevel, contextSemantics, objectSemantics)")
  public Object doSOMNode(final VirtualFrame frame,
                                    final Object receiver,
                                    final boolean executingBaseLevel,
                                    final Object contextSemantics,
                                    final boolean needObjectSemanticsCheck,
                                    final Object objectSemantics){
    return this.doBaseSOMNode(frame);
  }

  @Specialization
  public Object doMateDispatchWithContextSemantics(final VirtualFrame frame,
      final SReflectiveObject receiver,
      final boolean executingBaseLevel,
      final SMateEnvironment contextSemantics,
      final boolean needObjectSemanticsCheck,
      final Object objectSemantics){
    return this.doMateDispatchNode(frame, contextSemantics, receiver);
  }

  @Specialization(guards="needObjectSemanticsCheck")
  public Object doMateDispatchWithObjectSemantics(final VirtualFrame frame,
      final SReflectiveObject receiver,
      final boolean executingBaseLevel,
      final Object contextSemantics,
      final boolean needObjectSemanticsCheck,
      final SMateEnvironment objectSemantics){
    return this.doMateDispatchNode(frame, objectSemantics, receiver);
  }


  protected static boolean executeBaseLevel(final boolean executingBaseLevel, final Object contextSemantics, final Object objectSemantics){
    return (!executingBaseLevel) || (contextSemantics == null && objectSemantics == null);
  }

  public abstract static class MateMessageSendNode extends MateAbstractExpressionNode{
    @Child protected MateDispatchMessageLookup mateDispatch;

    public MateMessageSendNode(final ExpressionWithReceiverNode node) {
      super(node.getSourceSection());
      mateDispatch = MateDispatchMessageLookupNodeGen.create(node);
    }

    @Override
    protected Object doMateDispatchNode(final VirtualFrame frame, final SMateEnvironment environment, final SObject receiver){
      return this.mateDispatch.executeDispatch(frame, environment, receiver);
    }

    @Override
    protected Object doBaseSOMNode(final VirtualFrame frame){
      return this.mateDispatch.doWrappedNode(frame);
    }

    public AbstractMessageSendNode getSOMWrappedNode(){
      return this.mateDispatch.getSOMWrappedNode();
    }
  }

  public abstract static class MateFieldNode extends MateAbstractExpressionNode{
    @Child protected MateDispatchFieldAccess mateDispatch;

    public MateFieldNode(final FieldNode node) {
      super(node.getSourceSection());
      mateDispatch = MateDispatchFieldAccessNodeGen.create(node);
    }

    @Override
    protected Object doMateDispatchNode(final VirtualFrame frame, final SMateEnvironment environment, final SObject receiver){
      return this.mateDispatch.executeDispatch(frame, environment, receiver);
    }

    @Override
    protected Object doBaseSOMNode(final VirtualFrame frame){
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
