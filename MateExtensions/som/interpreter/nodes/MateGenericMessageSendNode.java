package som.interpreter.nodes;

import som.interpreter.nodes.MessageSendNode.GenericMessageSendNode;
import som.interpreter.nodes.dispatch.AbstractDispatchNode;
import som.matenodes.MateAbstractReflectiveDispatch.MateAbstractStandardDispatch;
import som.matenodes.MateAbstractReflectiveDispatchFactory.MateCachedDispatchMessageLookupNodeGen;
import som.matenodes.MateAbstractReflectiveDispatchFactory.MateDispatchMessageLookupNodeGen;
import som.matenodes.MateAbstractSemanticNodes.MateSemanticCheckNode;
import som.matenodes.MateBehavior;
import som.vm.MateSemanticsException;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;


public class MateGenericMessageSendNode extends GenericMessageSendNode implements MateBehavior {
  @Child MateSemanticCheckNode            semanticCheck;
  @Child MateAbstractStandardDispatch     reflectiveDispatch;

  protected MateGenericMessageSendNode(final SSymbol selector,
      final ExpressionNode[] arguments,
      final AbstractDispatchNode dispatchNode, final SourceSection source) {
    super(selector, arguments, dispatchNode, source);
    this.initializeMateNodes();
  }
  
  protected MateGenericMessageSendNode(GenericMessageSendNode somNode) {
    super(somNode.getSelector(), somNode.argumentNodes, somNode.getDispatchListHead(), somNode.getSourceSection());
    this.initializeMateNodes();
  }
  
  protected void initializeMateNodes(){
    semanticCheck = MateSemanticCheckNode.createForFullCheck(this.getSourceSection(), this.reflectiveOperation());
    reflectiveDispatch = MateCachedDispatchMessageLookupNodeGen.create(this.getSourceSection(), this.getSelector());
    //reflectiveDispatch = MateDispatchMessageLookupNodeGen.create(this.getSourceSection(), this.getSelector());
  }

  @Override
  public final Object executeGeneric(final VirtualFrame frame) {
    Object[] arguments = evaluateArguments(frame);
    try {
      return this.doMateSemantics(frame, arguments);
    } catch (MateSemanticsException e){
      return doPreEvaluated(frame, arguments);
    }
  }
  
  @Override
  public MateSemanticCheckNode getMateNode() {
    return semanticCheck;
  }

  @Override
  public MateAbstractStandardDispatch getMateDispatch() {
    return reflectiveDispatch;
  }
}
