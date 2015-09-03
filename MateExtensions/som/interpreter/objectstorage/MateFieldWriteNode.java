package som.interpreter.objectstorage;

import som.interpreter.nodes.MateFieldNode;
import som.interpreter.nodes.MateFieldNodeGen.MateWriteFieldNodeGen;
import som.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;
import som.vmobjects.SObject;


public class MateFieldWriteNode extends AbstractWriteFieldNode {
  //@Child AbstractWriteFieldNode baseNode;
  @Child MateFieldNode mateNode;
  
  public MateFieldWriteNode(AbstractWriteFieldNode base){
    super(base.getFieldIndex());
    //baseNode = base;
    mateNode = MateWriteFieldNodeGen.create(base);
  }
  
  public Object write(SObject obj, Object value){
    //return mateNode.execute(obj, this.getFieldIndex());
    return mateNode.execute(obj, value, this.getFieldIndex());
  } 
}
