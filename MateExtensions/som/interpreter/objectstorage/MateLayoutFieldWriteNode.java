package som.interpreter.objectstorage;

import som.interpreter.objectstorage.FieldAccessorNode.WriteFieldNode;
import som.matenodes.MateAbstractReflectiveDispatch.MateAbstractStandardDispatch;
import som.matenodes.MateAbstractSemanticNodes.MateSemanticCheckNode;
import som.matenodes.MateBehavior;
import som.vm.MateSemanticsException;
import som.vm.Universe;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;


public final class MateLayoutFieldWriteNode extends WriteFieldNode implements MateBehavior {
  @Child private MateSemanticCheckNode          semanticCheck;
  @Child private MateAbstractStandardDispatch   reflectiveDispatch;
  @Child private WriteFieldNode                 write;
  
  public MateLayoutFieldWriteNode(final WriteFieldNode node) {
    super(node.getFieldIndex());
    this.initializeMateSemantics(this.getSourceSection(), this.reflectiveOperation());
    this.initializeMateDispatchForFieldAccess(this.getSourceSection());
    write = node;
  }

  public Object write(final VirtualFrame frame, final DynamicObject receiver, final Object value) {
   try {
      return this.doMateSemantics(frame, new Object[] {receiver, (long) this.getFieldIndex(), value});
   } catch (MateSemanticsException e){
     return write.executeWrite(receiver, value);
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
  public Object executeWrite(DynamicObject obj, Object value) {
    /*Should never enter here*/
    assert(false);
    Universe.errorExit("Mate enters an unexpected method");
    return value;
  }
}