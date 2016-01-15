package som.interpreter.objectstorage;

import som.interpreter.objectstorage.FieldAccessorNode.ReadFieldNode;
import som.matenodes.MateBehavior;
import som.matenodes.MateAbstractReflectiveDispatch.MateAbstractStandardDispatch;
import som.matenodes.MateAbstractSemanticNodes.MateSemanticCheckNode;
import som.vm.MateSemanticsException;
import som.vm.Universe;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;


public final class MateLayoutFieldReadNode extends ReadFieldNode implements MateBehavior {
  @Child private MateSemanticCheckNode          semanticCheck;
  @Child private MateAbstractStandardDispatch   reflectiveDispatch;
  @Child private ReadFieldNode                  read;

  public MateLayoutFieldReadNode(final ReadFieldNode node) {
    super(node.getFieldIndex());
    this.initializeMateSemantics(this.getSourceSection(), this.reflectiveOperation());
    this.initializeMateDispatchForFieldAccess(this.getSourceSection());
    read = node;
  }

  public Object read(final VirtualFrame frame, final DynamicObject receiver) {
    try {
      return this.doMateSemantics(frame, new Object[] {receiver, (long)this.getFieldIndex()});
   } catch (MateSemanticsException e){
     return read.executeRead(receiver);
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
  public Object executeRead(DynamicObject obj) {
    /*Should never enter here*/
    assert(false);
    Universe.errorExit("Mate enters an unexpected method");
    return null;
  }
}