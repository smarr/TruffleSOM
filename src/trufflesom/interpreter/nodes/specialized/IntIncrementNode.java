package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.FieldNode.FieldReadNode;


@NodeChild(value = "rcvr", type = ExpressionNode.class)
public abstract class IntIncrementNode extends ExpressionNode {
  @Specialization(rewriteOn = ArithmeticException.class)
  public long doInc(final long rcvr) {
    return Math.addExact(rcvr, 1);
  }

  @Specialization
  public double doInc(final double rcvr) {
    return rcvr + 1;
  }

  public abstract ExpressionNode getRcvr();

  public boolean doesAccessField(final int fieldIdx) {
    ExpressionNode rcvr = getRcvr();
    if (rcvr instanceof FieldReadNode) {
      FieldReadNode r = (FieldReadNode) rcvr;
      return r.getFieldIndex() == fieldIdx;
    }

    return false;
  }
}
