package trufflesom.interpreter.supernodes.inc;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.FieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.IncrementLongFieldNode;
import trufflesom.vm.NotYetImplementedException;
import trufflesom.vmobjects.SObject;


public final class IncFieldWithExpNode extends FieldNode {
  @Child private ExpressionNode         self;
  @Child private ExpressionNode         valueExpr;
  @Child private IncrementLongFieldNode inc;

  IncFieldWithExpNode(final ExpressionNode self, final ExpressionNode valueExpr,
      final IncrementLongFieldNode inc, final long coord) {
    initialize(coord);
    this.self = self;
    this.valueExpr = valueExpr;
    this.inc = inc;
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
    return executeLong(frame);
  }

  @Override
  public long executeLong(final VirtualFrame frame) {
    SObject obj = (SObject) self.executeGeneric(frame);
    long incValue;

    try {
      incValue = valueExpr.executeLong(frame);
    } catch (UnexpectedResultException e) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      throw new NotYetImplementedException();
    }

    return inc.increment(obj, incValue);
  }
}
