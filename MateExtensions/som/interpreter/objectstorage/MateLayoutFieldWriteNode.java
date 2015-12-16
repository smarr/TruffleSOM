package som.interpreter.objectstorage;

import som.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;
import som.matenodes.MateAbstractReflectiveDispatch.MateAbstractStandardDispatch;
import som.matenodes.MateAbstractSemanticNodes.MateSemanticCheckNode;
import som.matenodes.MateBehavior;
import som.vm.MateSemanticsException;
import som.vmobjects.SObject;

import com.oracle.truffle.api.frame.VirtualFrame;


public class MateLayoutFieldWriteNode extends AbstractWriteFieldNode implements MateBehavior {
  @Child private MateSemanticCheckNode          semanticCheck;
  @Child private MateAbstractStandardDispatch   reflectiveDispatch;
  @Child private AbstractWriteFieldNode         write;
  
  public MateLayoutFieldWriteNode(final AbstractWriteFieldNode node) {
    super(node.getFieldIndex());
    this.initializeMateSemantics(this.getSourceSection(), this.reflectiveOperation());
    this.initializeMateDispatchForFieldAccess(this.getSourceSection());
    write = node;
  }

  public Object write(final VirtualFrame frame, final SObject receiver, final Object value) {
   try {
      return this.doMateSemantics(frame, new Object[] {receiver, (long) this.getFieldIndex(), value});
   } catch (MateSemanticsException e){
     return write.write(receiver, value);
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
  public void setMateNode(MateSemanticCheckNode node) {
    semanticCheck = node;
  }

  @Override
  public void setMateDispatch(MateAbstractStandardDispatch node) {
    reflectiveDispatch = node;
  }

  @Override
  public Object write(SObject obj, Object value) {
    return write.write(obj, value);
  }
  
  @Override
  public int lengthOfDispatchChain() {
    return 0;
  }
}