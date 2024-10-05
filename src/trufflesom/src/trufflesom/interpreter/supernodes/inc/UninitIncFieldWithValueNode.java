package trufflesom.interpreter.supernodes.inc;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.FieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.IncrementLongFieldNode;
import trufflesom.vm.NotYetImplementedException;
import trufflesom.vmobjects.SObject;


public class UninitIncFieldWithValueNode extends FieldNode {
  @Child private ExpressionNode self;
  private final int             fieldIndex;
  private final long            incValue;

  public UninitIncFieldWithValueNode(final ExpressionNode self, final int fieldIndex,
      final long coord, long incValue) {
    this.self = self;
    this.fieldIndex = fieldIndex;
    this.sourceCoord = coord;
    this.incValue = incValue;
  }

  public int getFieldIndex() {
    return fieldIndex;
  }

  @Override
  public ExpressionNode getSelf() {
    return self;
  }

  @Override
  public Object doPreEvaluated(final VirtualFrame frame, final Object[] arguments) {
    CompilerDirectives.transferToInterpreter();
    throw new UnsupportedOperationException();
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    CompilerDirectives.transferToInterpreterAndInvalidate();
    SObject obj = (SObject) self.executeGeneric(frame);

    Object val = obj.getField(fieldIndex);
    if (!(val instanceof Long)) {
      throw new NotYetImplementedException();
    }

    long longVal = 0;
    try {
      longVal = Math.addExact((Long) val, incValue);
      obj.setField(fieldIndex, longVal);
    } catch (ArithmeticException e) {
      throw new NotYetImplementedException();
    }

    IncrementLongFieldNode node = FieldAccessorNode.createIncrement(fieldIndex, obj);
    IncFieldWithValueNode incNode =
        new IncFieldWithValueNode(self, node, sourceCoord, incValue);
    replace(incNode);
    node.notifyAsInserted();

    return longVal;
  }
}
