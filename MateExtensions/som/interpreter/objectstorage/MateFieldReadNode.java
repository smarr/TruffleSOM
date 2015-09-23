/*package som.interpreter.objectstorage;

import som.interpreter.nodes.MateFieldNode;
import som.interpreter.nodes.MateFieldNodeGen.MateReadFieldNodeGen;
import som.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import som.vmobjects.SObject;


public class MateFieldReadNode extends AbstractReadFieldNode {
  //@Child AbstractReadFieldNode baseNode;
  @Child MateFieldNode mateNode;
  
  public MateFieldReadNode(AbstractReadFieldNode base){
    super(base.getFieldIndex());
    //baseNode = base;
    mateNode = MateReadFieldNodeGen.create(base);
  }
  
  public Object read(SObject obj){
    //return baseNode.read(obj);
    return mateNode.execute(obj, null, this.getFieldIndex());
  }
}
*/