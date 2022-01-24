package trufflesom.interpreter.supernodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.FieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.IncrementLongFieldNode;
import trufflesom.vmobjects.SObject;


final class IncFieldNode extends FieldNode {
  @Child private ExpressionNode         self;
  @Child private IncrementLongFieldNode inc;

  IncFieldNode(final ExpressionNode self, final IncrementLongFieldNode inc,
      final long coord) {
    initialize(coord);
    this.self = self;
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
    return inc.increment(obj);
  }
}
