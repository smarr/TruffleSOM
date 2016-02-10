package som.interpreter.nodes;

import som.interpreter.nodes.FieldNode.FieldReadNode;
import som.interpreter.nodes.FieldNode.FieldWriteNode;
import som.interpreter.objectstorage.MateLayoutFieldReadNode;
import som.interpreter.objectstorage.MateLayoutFieldWriteNode;
import som.matenodes.MateAbstractReflectiveDispatch.MateAbstractStandardDispatch;
import som.matenodes.MateAbstractSemanticNodes.MateSemanticCheckNode;
import som.matenodes.MateBehavior;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.object.basic.DynamicObjectBasic;


public abstract class MateFieldNodes {
  public static abstract class MateFieldReadNode extends FieldReadNode implements MateBehavior {
    @Child MateSemanticCheckNode            semanticCheck;
    @Child MateAbstractStandardDispatch     reflectiveDispatch;
    private final ConditionProfile semanticsRedefined = ConditionProfile.createBinaryProfile();

    public MateFieldReadNode(final FieldReadNode node) {
      super(node.read.getFieldIndex(), node.getSourceSection());
      this.initializeMateSemantics(this.getSourceSection(), this.reflectiveOperation());
      this.initializeMateDispatchForFieldAccess(this.getSourceSection());
      read = new MateLayoutFieldReadNode(read);
    }

    @Override
    @Specialization
    public Object executeEvaluated(final VirtualFrame frame, final DynamicObjectBasic obj) {
      Object value = this.doMateSemantics(frame, new Object[] {obj, (long) this.read.getFieldIndex()}, semanticsRedefined);
      if (value == null){
       value = ((MateLayoutFieldReadNode)read).read(frame, obj);
      }
      return value;
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
    public void setMateNode(final MateSemanticCheckNode node) {
      semanticCheck = node;
    }

    @Override
    public void setMateDispatch(final MateAbstractStandardDispatch node) {
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
    private final ConditionProfile semanticsRedefined = ConditionProfile.createBinaryProfile();

    public MateFieldWriteNode(final FieldWriteNode node) {
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
    public void setMateNode(final MateSemanticCheckNode node) {
      semanticCheck = node;
    }

    @Override
    public void setMateDispatch(final MateAbstractStandardDispatch node) {
      reflectiveDispatch = node;
    }

    @Override
    @Specialization
    public final Object executeEvaluated(final VirtualFrame frame,
        final DynamicObjectBasic self, final Object value) {
      Object val = this.doMateSemantics(frame, new Object[] {self, (long) this.write.getFieldIndex(), value}, semanticsRedefined);
      if (val == null){
       val = ((MateLayoutFieldWriteNode)write).write(frame, self, value);
      }
      return val;
    }

    @Override
    public ExpressionNode asMateNode() {
      return null;
    }
  }
}
