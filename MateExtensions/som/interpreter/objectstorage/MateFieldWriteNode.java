package som.interpreter.objectstorage;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleRuntime;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.impl.DefaultTruffleRuntime;
import com.oracle.truffle.api.nodes.Node.Child;

import som.interpreter.nodes.MateAbstractReflectiveDispatch;
import som.interpreter.nodes.MateAbstractSemanticCheckNode;
import som.interpreter.nodes.MateAbstractReflectiveDispatch.MateDispatchFieldAccessor;
import som.interpreter.nodes.MateAbstractSemanticCheckNode.MateEnvironmentSemanticCheckNode;
import som.interpreter.nodes.MateAbstractSemanticCheckNode.MateObjectSemanticCheckNode;
import som.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import som.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SObject;


public class MateFieldWriteNode extends AbstractWriteFieldNode {
  @Child protected MateEnvironmentSemanticCheckNode environment;
  @Child protected MateObjectSemanticCheckNode object;
  @Child protected MateDispatchFieldAccessor mateDispatch;
  
  public MateFieldWriteNode(AbstractReadFieldNode node) {
    super(node.getFieldIndex());
    environment = MateEnvironmentSemanticCheckNode.create();
    object = MateObjectSemanticCheckNode.create();
    mateDispatch = MateDispatchFieldWriteLayoutNodeGen.create(node);
  }
  
  @Override
  public Object write(SObject receiver, Object value) {
    VirtualFrame frame = (VirtualFrame) Truffle.getRuntime().getCurrentFrame().getFrame(FrameAccess.NONE, false);
    SMateEnvironment env = (SMateEnvironment)environment.executeGeneric(frame);
    Object[] args = {receiver, this.getFieldIndex()}; 
    if (env != null){
      return mateDispatch.executeDispatch(frame, env, args);
    } else {
      env = (SMateEnvironment)object.executeGeneric(frame, receiver);
      if (env != null){
        return mateDispatch.executeDispatch(frame, env, args);
      } else {
        return mateDispatch.doSomNode(frame, env, args);
      }
    }
  }
}