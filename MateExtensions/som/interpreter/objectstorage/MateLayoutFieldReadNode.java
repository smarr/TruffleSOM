package som.interpreter.objectstorage;

import som.interpreter.objectstorage.FieldAccessorNode.ReadFieldNode;
import som.matenodes.MateAbstractReflectiveDispatch.MateAbstractStandardDispatch;
import som.matenodes.MateAbstractSemanticNodes.MateSemanticCheckNode;
import som.matenodes.MateBehavior;
import som.vm.Universe;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;


public final class MateLayoutFieldReadNode extends ReadFieldNode implements MateBehavior {
  @Child private MateSemanticCheckNode          semanticCheck;
  @Child private MateAbstractStandardDispatch   reflectiveDispatch;
  @Child private ReadFieldNode                  read;
  private final ConditionProfile semanticsRedefined = ConditionProfile.createBinaryProfile();
  
  public MateLayoutFieldReadNode(final ReadFieldNode node) {
    super(node.getFieldIndex());
    this.initializeMateSemantics(this.getSourceSection(), this.reflectiveOperation());
    this.initializeMateDispatchForFieldAccess(this.getSourceSection());
    read = node;
  }

  public Object read(final VirtualFrame frame, final DynamicObject receiver) {
    Object value = this.doMateSemantics(frame, new Object[] {receiver, (long)this.getFieldIndex()}, semanticsRedefined);
    if (value == null){
     value = read.executeRead(receiver);
    }
    return value;
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