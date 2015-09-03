/*package som.interpreter.nodes;

import som.interpreter.SArguments;
import som.vm.MateUniverse;
import som.vm.constants.ReflectiveOp;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SInvokable.SMethod;
import som.vmobjects.SObject;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;


public abstract class MateFieldDispatch extends Node {
  @CompilationFinal protected final Node baseLevel;
  
  public MateFieldDispatch(final Node node){
    this.baseLevel = node;
  }
  
  public static MateFieldDispatch create(final Node node) {
    return MateFieldDispatchNodeGen.create(node);
  }
  
  public Node getBaseLevel(){
    return this.baseLevel;
  }
  
  public Object[] evaluateArguments(final VirtualFrame frame) {
    Object[] receiver = new Object[1];
    receiver[0] = SArguments.rcvr(frame);
    return receiver;
  }
   
  public abstract Object executeDispatch(Object receiver, Object argument, final int index, SMateEnvironment environment);
  
  @Specialization(guards = "cachedEnvironment==environment")
  public Object doMetaLevel(Object receiver,  
      Object arguments,
      int index,
      SMateEnvironment environment,
      @Cached("environment") SMateEnvironment cachedEnvironment,
      @Cached("createDispatch(environment, baseLevel)") SMethod[] reflectiveMethods) 
  { 
    if (reflectiveMethods == null)
      return null;
    else {
      MateUniverse.current().enterMetaExecutionLevel();
      Object value = reflectiveMethods[0].invoke();
      MateUniverse.current().leaveMetaExecutionLevel();
      return value;
    }
  }
  
  @Specialization
  public Object doBaseLevel(Object receiver,  
      Object arguments,
      int index,
      SMateEnvironment environment){
    return null;
  }
  
  public Object doMeta(final VirtualFrame frame, Object[] arguments, SMethod[] metaDelegation){
    MateUniverse.current().enterMetaExecutionLevel();
    Object value = metaDelegation[0].invoke(frame);
    MateUniverse.current().leaveMetaExecutionLevel();
    return value;
  }
  
  public SMethod[] createDispatch(SObject metaobject, Node node){
    ReflectiveOp operation = ((ExpressionNode) node).reflectiveOperation();
    return ((SMateEnvironment)metaobject).methodsImplementing(operation);
  }
}
*/