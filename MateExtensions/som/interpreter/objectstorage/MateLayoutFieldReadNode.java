package som.interpreter.objectstorage;

import som.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import som.matenodes.MateBehavior;
import som.matenodes.MateAbstractReflectiveDispatch.MateAbstractStandardDispatch;
import som.matenodes.MateAbstractReflectiveDispatchFactory.MateDispatchFieldAccessNodeGen;
import som.matenodes.MateAbstractSemanticNodes.MateSemanticCheckNode;
import som.vm.MateSemanticsException;
import som.vmobjects.SObject;

import com.oracle.truffle.api.frame.VirtualFrame;


public class MateLayoutFieldReadNode extends AbstractReadFieldNode implements MateBehavior {
  @Child private MateSemanticCheckNode          semanticCheck;
  @Child private MateAbstractStandardDispatch   reflectiveDispatch;
  @Child private AbstractReadFieldNode          read;

  public MateLayoutFieldReadNode(final AbstractReadFieldNode node) {
    super(node.getFieldIndex());
    semanticCheck = MateSemanticCheckNode.createForFullCheck(this.getSourceSection(), this.reflectiveOperation());
    reflectiveDispatch = MateDispatchFieldAccessNodeGen.create(this.getSourceSection());
    read = node;
  }

  public Object read(final VirtualFrame frame, final SObject receiver) {
    try {
      return this.doMateSemantics(frame, new Object[] {receiver, this.getFieldIndex()});
   } catch (MateSemanticsException e){
     return read.read(receiver);
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

  @Override
  public Object read(SObject obj) {
    return read.read(obj);
  }
  
  @Override
  public int lengthOfDispatchChain() {
    return 0;
  }
}