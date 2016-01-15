package som.interpreter.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import som.interpreter.SArguments;
import som.interpreter.nodes.ArgumentReadNode.LocalArgumentReadNode;
import som.interpreter.nodes.ArgumentReadNode.NonLocalArgumentReadNode;
import som.interpreter.nodes.ArgumentReadNode.LocalSuperReadNode;
import som.interpreter.nodes.ArgumentReadNode.NonLocalSuperReadNode;
import som.matenodes.MateAbstractReflectiveDispatch.MateAbstractStandardDispatch;
import som.matenodes.MateAbstractSemanticNodes.MateSemanticCheckNode;
import som.matenodes.MateBehavior;
import som.vm.MateSemanticsException;
import som.vmobjects.SSymbol;

public abstract class MateArgumentReadNode {
  public static class MateLocalArgumentReadNode extends LocalArgumentReadNode implements
      MateBehavior {
    @Child MateSemanticCheckNode            semanticCheck;
    @Child MateAbstractStandardDispatch     reflectiveDispatch;
    
    public MateLocalArgumentReadNode(int argumentIndex, SourceSection source) {
      super(argumentIndex, source);
      this.initializeMateSemantics(source, this.reflectiveOperation());
    }
    
    public MateLocalArgumentReadNode(LocalArgumentReadNode node) {
      super(node.argumentIndex, node.getSourceSection());
      this.initializeMateSemantics(node.getSourceSection(), this.reflectiveOperation());
    }
  
    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      try {
        return this.doMateSemantics(frame, new Object[] {SArguments.rcvr(frame)});
      } catch (MateSemanticsException e){
        return super.executeGeneric(frame);
      }
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
  }
  
  public static class MateNonLocalArgumentReadNode extends NonLocalArgumentReadNode implements
      MateBehavior {
    @Child MateSemanticCheckNode            semanticCheck;
    @Child MateAbstractStandardDispatch     reflectiveDispatch;
    
    public MateNonLocalArgumentReadNode(int argumentIndex, int contextLevel,
        SourceSection source) {
      super(argumentIndex, contextLevel, source);
      this.initializeMateSemantics(source, this.reflectiveOperation());
    }
    
    public MateNonLocalArgumentReadNode(NonLocalArgumentReadNode node) {
      super(node.argumentIndex, node.contextLevel, node.getSourceSection());
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
    public Object executeGeneric(final VirtualFrame frame) {
      try {
        return this.doMateSemantics(frame, new Object[] {SArguments.rcvr(frame)});
      } catch (MateSemanticsException e){
        return super.executeGeneric(frame);
      }
    }
  }
  
  public static final class MateLocalSuperReadNode extends LocalSuperReadNode implements 
      ISuperReadNode, MateBehavior {
    @Child MateSemanticCheckNode            semanticCheck;
    @Child MateAbstractStandardDispatch     reflectiveDispatch;
    
    public MateLocalSuperReadNode(SSymbol holderClass, boolean classSide,
        SourceSection source) {
      super(holderClass, classSide, source);
      this.initializeMateSemantics(source, this.reflectiveOperation());
    }

    public MateLocalSuperReadNode(LocalSuperReadNode node) {
      super(node.getHolderClass(), node.isClassSide(), node.getSourceSection());
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
    public Object executeGeneric(final VirtualFrame frame) {
      try {
        return this.doMateSemantics(frame, new Object[] {SArguments.rcvr(frame)});
      } catch (MateSemanticsException e){
        return super.executeGeneric(frame);
      }
    }
  }
  
  public static final class MateNonLocalSuperReadNode extends NonLocalSuperReadNode implements 
      ISuperReadNode, MateBehavior {

    @Child MateSemanticCheckNode            semanticCheck;
    @Child MateAbstractStandardDispatch     reflectiveDispatch;
    
    public MateNonLocalSuperReadNode(int contextLevel, SSymbol holderClass,
        boolean classSide, SourceSection source) {
      super(contextLevel, holderClass, classSide, source);
      this.initializeMateSemantics(source, this.reflectiveOperation());
    }
    
    public MateNonLocalSuperReadNode(NonLocalSuperReadNode node) {
      super(node.getContextLevel(), node.getHolderClass(), node.isClassSide(), node.getSourceSection());
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
    public Object executeGeneric(final VirtualFrame frame) {
      try {
        return this.doMateSemantics(frame, new Object[] {SArguments.rcvr(frame)});
      } catch (MateSemanticsException e){
        return super.executeGeneric(frame);
      }
    }
  }  
}