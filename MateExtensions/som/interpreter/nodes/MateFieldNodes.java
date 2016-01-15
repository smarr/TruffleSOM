package som.interpreter.nodes;

import som.interpreter.nodes.FieldNode.FieldReadNode;
import som.interpreter.nodes.FieldNode.FieldWriteNode;
import som.interpreter.objectstorage.MateLayoutFieldReadNode;
import som.interpreter.objectstorage.MateLayoutFieldWriteNode;
import som.matenodes.MateAbstractReflectiveDispatch.MateAbstractStandardDispatch;
import som.matenodes.MateAbstractSemanticNodes.MateSemanticCheckNode;
import som.matenodes.MateBehavior;
import som.vm.MateSemanticsException;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;


public abstract class MateFieldNodes {
  public static abstract class MateFieldReadNode extends FieldReadNode implements MateBehavior {
    @Child MateSemanticCheckNode            semanticCheck;
    @Child MateAbstractStandardDispatch     reflectiveDispatch;
    
    public MateFieldReadNode(FieldReadNode node) {
      super(node.read.getFieldIndex(), node.getSourceSection());
      this.initializeMateSemantics(this.getSourceSection(), this.reflectiveOperation());
      this.initializeMateDispatchForFieldAccess(this.getSourceSection());
      read = new MateLayoutFieldReadNode(read);
    }
    
    //Todo: Check a better option for removing the silly guard
    @Override
    @Specialization
    public Object executeEvaluated(final VirtualFrame frame, final DynamicObject obj) {
      try {
         return this.doMateSemantics(frame, new Object[] {obj, (long) this.read.getFieldIndex()});
      } catch (MateSemanticsException e){
        return ((MateLayoutFieldReadNode)read).read(frame, obj);
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
    
    @Override
    public ExpressionNode asMateNode() {
      return null;
    }
  }
  
  public static abstract class MateFieldWriteNode extends FieldWriteNode implements MateBehavior {
    @Child MateSemanticCheckNode            semanticCheck;
    @Child MateAbstractStandardDispatch     reflectiveDispatch;
    
    public MateFieldWriteNode(FieldWriteNode node) {
      super(node.write.getFieldIndex(), node.getSourceSection());
      this.initializeMateSemantics(this.getSourceSection(), this.reflectiveOperation());
      this.initializeMateDispatchForFieldAccess(this.getSourceSection());
      write = new MateLayoutFieldWriteNode(write);
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
    @Specialization
    public final Object executeEvaluated(final VirtualFrame frame,
        final DynamicObject self, final Object value) {
      try {
         return this.doMateSemantics(frame, new Object[] {self, (long) this.write.getFieldIndex(), value});
      } catch (MateSemanticsException e){
        return ((MateLayoutFieldWriteNode)write).write(frame, self, value);
      }
    }
    
    @Override
    public ExpressionNode asMateNode() {
      return null;
    }
  }
}
