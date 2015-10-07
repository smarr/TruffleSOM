package som.interpreter.objectstorage;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.VirtualFrame;

import som.interpreter.nodes.MateAbstractReflectiveDispatchFactory.MateDispatchFieldReadLayoutNodeGen;
import som.interpreter.nodes.MateAbstractReflectiveDispatch.MateDispatchFieldAccessor;
import som.interpreter.nodes.MateAbstractSemanticCheckNode.MateEnvironmentSemanticCheckNode;
import som.interpreter.nodes.MateAbstractSemanticCheckNode.MateObjectSemanticCheckNode;
import som.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import som.vm.MateUniverse;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SObject;


public class MateFieldReadNode extends AbstractReadFieldNode {
  @Child protected MateEnvironmentSemanticCheckNode environment;
  @Child protected MateObjectSemanticCheckNode object;
  @Child protected MateDispatchFieldAccessor mateDispatch;

  public MateFieldReadNode(final AbstractReadFieldNode node) {
    super(node.getFieldIndex());
    environment = MateEnvironmentSemanticCheckNodeGen.create();
    object = MateObjectSemanticCheckNodeGen.create();
    mateDispatch = MateDispatchFieldReadLayoutNodeGen.create(node);
  }

  @Override
  public Object read(final SObject receiver) {
    VirtualFrame frame = (VirtualFrame) Truffle.getRuntime().getCallerFrame().getFrame(FrameAccess.READ_WRITE, false);
    Object[] args = {receiver, (long)this.getFieldIndex()};
    SMateEnvironment env = null;
    if (!MateUniverse.current().executingMeta()){
      env = (SMateEnvironment)environment.executeGeneric(frame);
      if (env == null){
        env = (SMateEnvironment)object.executeGeneric(frame, receiver);
      }
    }
    return mateDispatch.executeDispatch(frame, env, args);
  }
}