package som.interpreter.nodes;

import som.vm.MateUniverse;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SReflectiveObject;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public interface MateNode {
  
  public default Object metaExecution(VirtualFrame frame){
    Object[] args = this.getArguments().clone();
    this.setArguments(null);
    return this.getReflectiveDispatch().executeDispatch(frame, args, this.getEnvironment());
  }
  
  public default Object baseExecution(VirtualFrame frame){
    Object[] args = this.getArguments().clone();
    this.setArguments(null);
    return this.getReflectiveDispatch().doBase(frame, args);
  }
  
  public default boolean hasReflectiveBehavior(VirtualFrame frame){
    this.refreshArguments(frame);
    if (MateUniverse.current().executingMeta()) return false;
    FrameSlot slot = frame.getFrameDescriptor().findFrameSlot("semantics");
    if (slot != null) {
      try {
        if (frame.getObject(slot) instanceof SMateEnvironment){
          this.setEnvironment((SMateEnvironment)frame.getObject(slot));
          return true;
        }
      } catch (FrameSlotTypeException e) {
        e.printStackTrace();
      }
    }
    //Need this check because of the possibility to receive primitive types 
    Object receiver = this.getArguments()[0];
    if (receiver instanceof SReflectiveObject){
      SMateEnvironment environment = ((SReflectiveObject)receiver).getEnvironment();
      this.setEnvironment(environment);
      return environment != null;
    } else {
      return false;
    }
  }
  
  public default void refreshArguments(VirtualFrame frame){
    if (this.getArguments() == null){
      this.setArguments(this.evaluateArguments(frame));
    }
  }
  
  public default Node getOriginalNode(){
    return this.getReflectiveDispatch().getBaseLevel();
  }
  
  public Object[] evaluateArguments(final VirtualFrame frame);
    
  public MateDispatch getReflectiveDispatch();
  
  public void setEnvironment(SMateEnvironment environment);
  
  public SMateEnvironment getEnvironment();
  
  public Object[] getArguments();
  
  public void setArguments(Object[] arguments);
  
  
}
