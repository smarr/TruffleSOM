package som.interpreter.nodes;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import som.interpreter.TypesGen;
import som.interpreter.nodes.FieldNode.FieldReadNode;
import som.interpreter.nodes.FieldNode.FieldWriteNode;
import som.matenodes.MateAbstractReflectiveDispatch;
import som.matenodes.MateBehavior;
import som.matenodes.MateAbstractReflectiveDispatchNodeGen.MateDispatchFieldAccessNodeGen;
import som.matenodes.MateAbstractSemanticNodes.MateSemanticCheckNode;
import som.vm.MateSemanticsException;
import som.vmobjects.SObject;


public abstract class MateFieldNodes {
  public static final class MateFieldReadNode extends FieldReadNode implements MateBehavior {
    @Child MateSemanticCheckNode                   semanticCheck;
    @Child MateAbstractReflectiveDispatch     reflectiveDispatch;
    
    public MateFieldReadNode(FieldReadNode node) {
      super(node);
      semanticCheck = MateSemanticCheckNode.createForFullCheck(this.getSourceSection(), this.reflectiveOperation());
      reflectiveDispatch = MateDispatchFieldAccessNodeGen.create(this.getSourceSection());
    }
    
    @Override
    public Object executeEvaluated(final VirtualFrame frame, final SObject obj) {
      try {
         return this.doMateSemantics(frame, new Object[] {obj, this.read.getFieldIndex()});
      } catch (MateSemanticsException e){
        return super.executeEvaluated(frame, obj);
      }
    }
    
    @Override
    public long executeLong(final VirtualFrame frame) throws UnexpectedResultException {
      SObject obj = this.getSelf().executeSObject(frame);
      try {
        return TypesGen.expectLong(this.doMateSemantics(frame, new Object[] {obj, this.read.getFieldIndex()}));
      } catch (MateSemanticsException e){
        return super.executeLong(frame);
      }
    }

    @Override
    public double executeDouble(final VirtualFrame frame) throws UnexpectedResultException {
      SObject obj = this.getSelf().executeSObject(frame);
      try {
        return TypesGen.expectDouble(this.doMateSemantics(frame, new Object[] {obj, this.read.getFieldIndex()}));
      } catch (MateSemanticsException e){
        return super.executeDouble(frame);
      }
    }


    @Override
    public MateSemanticCheckNode getMateNode() {
      return semanticCheck;
    }

    @Override
    public MateAbstractReflectiveDispatch getMateDispatch() {
      return reflectiveDispatch;
    }
  }
  
  public static abstract class MateFieldWriteNode extends FieldWriteNode implements MateBehavior {
    @Child MateSemanticCheckNode                   semanticCheck;
    @Child MateAbstractReflectiveDispatch     reflectiveDispatch;
    
    public MateFieldWriteNode(FieldWriteNode node) {
      super(node);
      semanticCheck = MateSemanticCheckNode.createForFullCheck(this.getSourceSection(), this.reflectiveOperation());
      reflectiveDispatch = MateDispatchFieldAccessNodeGen.create(this.getSourceSection());
    }

    @Override
    public MateSemanticCheckNode getMateNode() {
      return semanticCheck;
    }

    @Override
    public MateAbstractReflectiveDispatch getMateDispatch() {
      return reflectiveDispatch;
    }
    
    @Override
    public final Object doEvaluated(final VirtualFrame frame,
        final SObject self, final Object value) {
      try {
         return this.doMateSemantics(frame, new Object[] {self, this.write.getFieldIndex(), value});
      } catch (MateSemanticsException e){
        return super.doEvaluated(frame, self, value);
      }
    }
    
    @Override
    @Specialization
    public long doLong(final VirtualFrame frame, final SObject self,
        final long value) {
      try {
        /*Todo: There is a bug here in the cases where the metaobject returns a value with a different type*/
        return (long) this.doMateSemantics(frame, new Object[] {self, this.write.getFieldIndex(), value});
      } catch (MateSemanticsException e){
        return super.doLong(frame, self, value);
      }
    }

    @Override
    @Specialization
    public double doDouble(final VirtualFrame frame, final SObject self,
        final double value) {
      try {
        /*Todo: There is a bug here in the cases where the metaobject returns a value with a different type*/
        return (double) this.doMateSemantics(frame, new Object[] {self, this.write.getFieldIndex(), value});
      } catch (MateSemanticsException e){
        return super.doDouble(frame, self, value);
      }
    }
    
    @Override
    @Specialization
    public Object doObject(final VirtualFrame frame, final SObject self,
        final Object value) {
      return doEvaluated(frame, self, value);
    }
  }
}
