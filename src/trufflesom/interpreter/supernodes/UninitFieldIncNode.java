package trufflesom.interpreter.supernodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.FieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.IncrementLongFieldNode;
import trufflesom.vm.NotYetImplementedException;
import trufflesom.vmobjects.SObject;


public final class UninitFieldIncNode extends FieldNode {

  @Child private ExpressionNode self;
  private final int             fieldIndex;
  private final long            incValue;

  public UninitFieldIncNode(final ExpressionNode self, final int fieldIndex,
      final long coord, final long value) {
    this.self = self;
    this.fieldIndex = fieldIndex;
    this.sourceCoord = coord;
    this.incValue = value;
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

    IncrementLongFieldNode node = FieldAccessorNode.createIncrement(fieldIndex, obj, incValue);
    IncFieldNode incNode = new IncFieldNode(self, node, sourceCoord);
    replace(incNode);
    node.notifyAsInserted();

    return longVal;
  }
}
