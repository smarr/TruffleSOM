package som.interpreter.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import som.interpreter.SArguments;
import som.interpreter.nodes.LocalVariableNode.LocalVariableReadNode;
import som.interpreter.nodes.LocalVariableNode.LocalVariableWriteNode;
import som.matenodes.MateAbstractReflectiveDispatch.MateAbstractStandardDispatch;
import som.matenodes.MateAbstractSemanticNodes.MateSemanticCheckNode;
import som.matenodes.MateBehavior;
import som.vm.MateSemanticsException;

public abstract class MateLocalVariableNode {
  public static class MateLocalVariableReadNode extends LocalVariableReadNode implements
      MateBehavior {
    
    public MateLocalVariableReadNode(LocalVariableReadNode node) {
      super(node);
      this.local = node;
      this.initializeMateSemantics(node.getSourceSection(), this.reflectiveOperation());
    }

    @Child MateSemanticCheckNode            semanticCheck;
    @Child MateAbstractStandardDispatch     reflectiveDispatch;
    @Child LocalVariableNode                local;
    
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
    public Object executeGeneric(VirtualFrame frame) {
      try {
        return this.doMateSemantics(frame, new Object[] {SArguments.rcvr(frame)});
      } catch (MateSemanticsException e){
        return local.executeGeneric(frame);
      }
    }
  }
  
  public static class MateLocalVariableWriteNode extends LocalVariableWriteNode implements
      MateBehavior {
    
    @Child MateSemanticCheckNode            semanticCheck;
    @Child MateAbstractStandardDispatch     reflectiveDispatch;
    @Child LocalVariableWriteNode           local;
    
    public MateLocalVariableWriteNode(LocalVariableWriteNode node) {
      super(node);
      this.local = node;
      this.initializeMateSemantics(node.getSourceSection(), this.reflectiveOperation());
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
    public Object executeGeneric(VirtualFrame frame) {
      try {
        return this.doMateSemantics(frame, new Object[] {SArguments.rcvr(frame)});
      } catch (MateSemanticsException e){
        return local.executeGeneric(frame);
      }
    }

    @Override
    public ExpressionNode getExp() {
      return local.getExp();
    }
  }
}
