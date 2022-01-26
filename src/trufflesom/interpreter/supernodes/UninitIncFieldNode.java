package trufflesom.interpreter.supernodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.FieldNode;
import trufflesom.interpreter.nodes.FieldNodeFactory.FieldWriteNodeGen;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.IncrementLongFieldNode;
import trufflesom.primitives.arithmetic.AdditionPrim;
import trufflesom.primitives.arithmetic.AdditionPrimFactory;
import trufflesom.vm.NotYetImplementedException;
import trufflesom.vmobjects.SObject;


public final class UninitIncFieldNode extends FieldNode {

  @Child private ExpressionNode self;
  @Child private ExpressionNode valueExpr;

  private final int     fieldIndex;
  private final boolean valueExprIsArg;

  public UninitIncFieldNode(final ExpressionNode self, final ExpressionNode valueExpr,
      final boolean valueExprIsArg, final int fieldIndex, final long coord) {
    this.self = self;
    this.valueExpr = valueExpr;
    this.valueExprIsArg = valueExprIsArg;
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
      AdditionPrim add;
      if (valueExprIsArg) {
        add = AdditionPrimFactory.create(
            new FieldReadNode((ExpressionNode) self.copy(), fieldIndex),
            valueExpr);
      } else {
        add = AdditionPrimFactory.create(
            valueExpr,
            new FieldReadNode((ExpressionNode) self.copy(), fieldIndex));
      }
      add.initialize(sourceCoord);

      replace(FieldWriteNodeGen.create(fieldIndex, self, add)
                               .initialize(sourceCoord)).adoptChildren();

      Object preIncValue = obj.getField(fieldIndex);
      Object result = add.executeEvaluated(frame, preIncValue, e1.getResult());
      obj.setField(fieldIndex, result);
      return result;
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
