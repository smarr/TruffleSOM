package som.interpreter.nodes;

import som.interpreter.nodes.MateAbstractSemanticCheckNodeFactory.MateEnvironmentSemanticCheckNodeGen;
import som.interpreter.nodes.MateAbstractSemanticCheckNodeFactory.MateObjectSemanticCheckNodeGen;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SReflectiveObject;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;


public abstract class MateAbstractSemanticCheckNode extends Node {

    public static abstract class MateEnvironmentSemanticCheckNode extends MateAbstractSemanticCheckNode {

    public abstract Object executeGeneric(VirtualFrame frame);
    public static MateEnvironmentSemanticCheckNode create(){
      return MateEnvironmentSemanticCheckNodeGen.create();
    }

    @Specialization(guards="semanticsFromSlot(frame) != null")
    public Object doSemanticsInFrame(
        final VirtualFrame frame,
        @Cached("semanticsFromSlot(frame)") final FrameSlot slot){
        try {
          return frame.getObject(slot);
        }
        catch (FrameSlotTypeException e) {
          return null;
        }
    }

    @Specialization(guards="semanticsFromSlot(frame) == null")
    public Object doNoSemanticsInFrame(final VirtualFrame frame){
      return null;
    }

    public static FrameSlot semanticsFromSlot(final VirtualFrame frame){
      return frame.getFrameDescriptor().findFrameSlot("semantics");
    }
  }

  public static abstract class MateObjectSemanticCheckNode extends MateAbstractSemanticCheckNode {
    public static MateObjectSemanticCheckNode create(){
      return MateObjectSemanticCheckNodeGen.create();
    }

    public abstract Object executeGeneric(VirtualFrame frame, Object receiver);

    @Specialization
    public Object doSReflectiveObject(
        final VirtualFrame frame, final SReflectiveObject receiver){
      return receiver.getEnvironment();
    }

    @Specialization
    public Object doSObject(final VirtualFrame frame, final Object receiver){
      return null;
    }
  }
}
