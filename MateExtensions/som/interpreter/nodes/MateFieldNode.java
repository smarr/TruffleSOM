package som.interpreter.nodes;

import som.interpreter.nodes.MateDispatch.MateDispatchFieldReadLayout;
import som.interpreter.nodes.MateDispatch.MateDispatchFieldWriteLayout;
import som.interpreter.objectstorage.FieldAccessorNode;
import som.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import som.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SObject;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

public abstract class MateFieldNode extends SOMNode implements MateNode {
  @Child protected MateDispatch reflectiveDispatch;
  protected Object[] arguments;
  protected SMateEnvironment environment;
  
  public MateFieldNode(final FieldAccessorNode node) {
    super(node.getSourceSection());
    if (node instanceof AbstractReadFieldNode){
      reflectiveDispatch = MateDispatchFieldReadLayout.create(node);
    } else {
      reflectiveDispatch = MateDispatchFieldWriteLayout.create(node);
    }
  }
  
  @Override
  public ExpressionNode getFirstMethodBodyNode() { return (ExpressionNode)this.getReflectiveDispatch().getBaseLevel(); }

  public abstract Object execute(SObject rcvr, Object arg, int index);
  
  @Specialization(guards="hasReflectiveBehavior(obj)")
  public Object doMetaLevel(SObject obj, Object arg, int index){
    Object[] arguments = {obj, (long)index, arg};
    return this.reflectiveDispatch.executeDispatch(null, arguments, environment);
  }
  
  public void setEnvironment(SMateEnvironment env){
    environment = env;
  }
  
  public SMateEnvironment getEnvironment(){
    return environment;
  }
  
  public MateDispatch getReflectiveDispatch(){
    return this.reflectiveDispatch;
  }
  
  public Object[] getArguments(){
    return arguments;
  }
  
  public void setArguments(Object[] args){
    arguments = args;
  }
  
  public Object[] evaluateArguments(final VirtualFrame frame) {
    arguments = this.reflectiveDispatch.evaluateArguments(frame);
    return arguments;
  }
  
  public abstract static class MateReadFieldNode extends MateFieldNode {
    public MateReadFieldNode(FieldAccessorNode node) {
      super(node);
    }

    @Specialization(guards="!hasReflectiveBehavior(obj)")
    public Object doBaseLevel(SObject obj, Object arg, int index) {
      return (((AbstractReadFieldNode) this.reflectiveDispatch.getBaseLevel()).read(obj));
    }
  }
  
  public abstract static class MateWriteFieldNode extends MateFieldNode {
    public MateWriteFieldNode(FieldAccessorNode node) {
      super(node);
    }

    @Specialization(guards="!hasReflectiveBehavior(obj)")
    public Object doBaseLevel(SObject obj, Object arg, int index) {
      return (((AbstractWriteFieldNode) this.reflectiveDispatch.getBaseLevel()).write(obj, arg));
    }
  }
}