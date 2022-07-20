package trufflesom.interpreter.supernodes;

import com.oracle.truffle.api.bytecode.OperationProxy.Proxyable;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.interpreter.Method.OpBuilder;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.FieldNode.FieldReadNode;


@Proxyable
@NodeChild(value = "rcvr", type = ExpressionNode.class)
public abstract class IntIncrementNode extends ExpressionNode {
  @Specialization(rewriteOn = ArithmeticException.class)
  public static long doInc(final long rcvr) {
    return Math.addExact(rcvr, 1);
  }

  @Specialization
  public static double doInc(final double rcvr) {
    return rcvr + 1;
  }

  public abstract Object executeEvaluated(Object rcvr);

  @Override
  public final Object doPreEvaluated(final VirtualFrame frame, final Object[] args) {
    return executeEvaluated(args[0]);
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

  @Override
  public void constructOperation(final OpBuilder opBuilder) {
    opBuilder.dsl.beginIntIncrement();
    getRcvr().accept(opBuilder);
    opBuilder.dsl.endIntIncrement();
  }
}
