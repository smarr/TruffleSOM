package trufflesom.interpreter.supernodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.FieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.IncrementLongFieldNode;
import trufflesom.vm.NotYetImplementedException;
import trufflesom.vmobjects.SObject;


public final class UninitIncFieldNode extends FieldNode {

  @Child private ExpressionNode self;
  @Child private ExpressionNode valueExpr;

  private final int fieldIndex;

  public UninitIncFieldNode(final ExpressionNode self, final ExpressionNode valueExpr,
      final int fieldIndex,
      final long coord) {
    this.self = self;
    this.valueExpr = valueExpr;
    this.fieldIndex = fieldIndex;
    this.sourceCoord = coord;
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

    long incValue;
    try {
      incValue = valueExpr.executeLong(frame);
    } catch (UnexpectedResultException e1) {
      throw new NotYetImplementedException();
    }

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
    IncFieldNode incNode = new IncFieldNode(self, valueExpr, node, sourceCoord);
    replace(incNode);
    node.notifyAsInserted();

    return longVal;
  }
}
