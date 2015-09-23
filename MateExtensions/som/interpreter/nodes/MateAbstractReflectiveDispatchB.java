package som.interpreter.nodes;
/*package som.interpreter.nodes;

import som.vmobjects.SMateEnvironment;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;


public abstract class MateAbstractReflectiveDispatch extends Node {
  public abstract Object executeDispatch(final VirtualFrame frame, Object receiver, SMateEnvironment environment);
  
  @Specialization(guards = "(cachedEnvironment==environment)")
  public Object doMetaLevel(final VirtualFrame frame,  
      Object receiver, 
      SMateEnvironment environment,
      @Cached("environment") SMateEnvironment cachedEnvironment)
      //@Cached("createDispatch(environment, baseLevel)") SMethod[] reflectiveMethods) 
  { 
    /*if (reflectiveMethods == null)
      return doBase(frame, arguments);
    else {
      return this.doMeta(frame, arguments, reflectiveMethods);
    }*/
   /* return null;
  }
  
  @Specialization()
  public Object doBaseLevel(final VirtualFrame frame, Object receiver, SMateEnvironment environment){
    //return this.doBase(frame, arguments);
    return null;
  }
  
  public abstract class MateAbstractReflectiveDispatch extends Node {
}
*/