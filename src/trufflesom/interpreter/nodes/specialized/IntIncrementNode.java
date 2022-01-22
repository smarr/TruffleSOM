package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.interpreter.bc.RespecializeException;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.FieldNode;
import trufflesom.interpreter.nodes.FieldNode.FieldReadNode;
import trufflesom.interpreter.nodes.FieldNode.UninitFieldIncNode;
import trufflesom.interpreter.nodes.GenericMessageSendNode;
import trufflesom.interpreter.nodes.MessageSendNode;
import trufflesom.interpreter.nodes.bc.BytecodeLoopNode;
import trufflesom.interpreter.nodes.literals.IntegerLiteralNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.VmSettings;
import trufflesom.vmobjects.SSymbol;


@NodeChild(value = "rcvr", type = ExpressionNode.class)
public abstract class IntIncrementNode extends ExpressionNode {

  protected final long    value;
  protected final boolean isMinusAndValueNegated;

  public IntIncrementNode(final long value, final boolean isMinusAndValueNegated) {
    this.value = value;
    this.isMinusAndValueNegated = isMinusAndValueNegated;
  }

  public abstract Object executeEvaluated(VirtualFrame frame, Object rcvr);

  @Override
  public final Object doPreEvaluated(final VirtualFrame frame, final Object[] args) {
    return executeEvaluated(frame, args[0]);
  }

  public abstract ExpressionNode getRcvr();

  @Specialization(rewriteOn = ArithmeticException.class)
  public long doInc(final long rcvr) {
    return Math.addExact(rcvr, value);
  }

  @Specialization
  public double doInc(final double rcvr) {
    return rcvr + value;
  }

  public abstract Object executeEvaluated(Object rcvr);

  @Override
  public final Object doPreEvaluated(final VirtualFrame frame, final Object[] args) {
    return executeEvaluated(args[0]);
  }

  @Fallback
  public final Object makeGenericSend(final VirtualFrame frame, final Object rcvr) {
    CompilerDirectives.transferToInterpreterAndInvalidate();
    return makeGenericSend().doPreEvaluated(frame,
        new Object[] {rcvr, isMinusAndValueNegated ? -value : value});
  }

  public boolean doesAccessField(final int fieldIdx) {
    ExpressionNode rcvr = getRcvr();
    if (rcvr instanceof FieldReadNode) {
      FieldReadNode r = (FieldReadNode) rcvr;
      return r.getFieldIndex() == fieldIdx;
    }

    return false;
  }

  protected GenericMessageSendNode makeGenericSend() {
    CompilerDirectives.transferToInterpreterAndInvalidate();
    ExpressionNode[] children;
    if (VmSettings.UseAstInterp) {
      children = new ExpressionNode[] {getRcvr(),
          new IntegerLiteralNode(isMinusAndValueNegated ? -value : value)};
    } else {
      children = null;
    }

    SSymbol selector =
        isMinusAndValueNegated ? SymbolTable.symbolFor("-") : SymbolTable.symbolFor("+");
    GenericMessageSendNode send =
        MessageSendNode.createGeneric(selector, children, sourceCoord);

    if (VmSettings.UseAstInterp) {
      replace(send);
      send.notifyDispatchInserted();
      return send;
    }

    assert getParent() instanceof BytecodeLoopNode : "This node was expected to be a direct child of a `BytecodeLoopNode`.";
    throw new RespecializeException(send);
  }

  public FieldNode createFieldIncNode(final ExpressionNode self, final int fieldIndex,
      final long coord) {
    return new UninitFieldIncNode(self, fieldIndex, coord, value);
  }

}
