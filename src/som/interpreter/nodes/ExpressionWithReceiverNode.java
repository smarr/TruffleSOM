package som.interpreter.nodes;

import som.interpreter.nodes.MateAbstractExpressionNodeGen.MateMessageSendNodeGen;
import som.interpreter.nodes.MateAbstractReceiverNode.MateReceiverExpressionNode;
import som.interpreter.nodes.MateAbstractSemanticCheckNodeFactory.MateEnvironmentSemanticCheckNodeGen;
import som.interpreter.nodes.MateAbstractSemanticCheckNodeFactory.MateObjectSemanticCheckNodeGen;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;


public abstract class ExpressionWithReceiverNode extends ExpressionNode {

  public ExpressionWithReceiverNode(final SourceSection sourceSection) {
    super(sourceSection);
  }

  public abstract ExpressionNode getReceiver();

  public Object evaluateReceiver(final VirtualFrame frame){
    return this.getReceiver().executeGeneric(frame);
  }

  @Override
  public void wrapIntoMateNode() {
    replace(MateMessageSendNodeGen.create(this,
        new MateReceiverExpressionNode(this),
        MateEnvironmentSemanticCheckNodeGen.create(),
        MateObjectSemanticCheckNodeGen.create()));
  }
}


