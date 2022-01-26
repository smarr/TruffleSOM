package trufflesom.interpreter.supernodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.compiler.Variable;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.bc.RespecializeException;
import trufflesom.interpreter.nodes.AbstractMessageSendNode;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.FieldNode;
import trufflesom.interpreter.nodes.FieldNode.FieldReadNode;
import trufflesom.interpreter.nodes.LocalVariableNode;
import trufflesom.interpreter.nodes.MessageSendNode;
import trufflesom.interpreter.nodes.NonLocalVariableNode;
import trufflesom.interpreter.nodes.bc.BytecodeLoopNode;
import trufflesom.interpreter.nodes.literals.IntegerLiteralNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.VmSettings;
import trufflesom.vmobjects.SSymbol;


@NodeChild(value = "rcvr", type = ExpressionNode.class)
public abstract class IntIncrementNode extends ExpressionNode {

  protected final long    incValue;
  protected final boolean isMinusAndValueNegated;

  public IntIncrementNode(final long value, final boolean isMinusAndValueNegated) {
    this.incValue = value;
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
    return Math.addExact(rcvr, incValue);
  }

  @Specialization
  public double doInc(final double rcvr) {
    return rcvr + incValue;
  }

  @Fallback
  public final Object makeGenericSend(final VirtualFrame frame, final Object rcvr) {
    CompilerDirectives.transferToInterpreterAndInvalidate();
    return makeGenericSend().doPreEvaluated(frame,
        new Object[] {rcvr, isMinusAndValueNegated ? -incValue : incValue});
  }

  public boolean doesAccessField(final int fieldIdx) {
    ExpressionNode rcvr = getRcvr();
    if (rcvr instanceof FieldReadNode) {
      FieldReadNode r = (FieldReadNode) rcvr;
      return r.getFieldIndex() == fieldIdx;
    }

    return false;
  }

  public boolean doesAccessVariable(final Variable var) {
    ExpressionNode rcvr = getRcvr();
    Local local;
    if (rcvr instanceof LocalVariableNode) {
      local = ((LocalVariableNode) rcvr).getLocal();
    } else if (rcvr instanceof NonLocalVariableNode) {
      local = ((NonLocalVariableNode) rcvr).getLocal();
    } else {
      return false;
    }
    return local.equals(var);
  }

  protected AbstractMessageSendNode makeGenericSend() {
    CompilerDirectives.transferToInterpreterAndInvalidate();

    SSymbol selector =
        isMinusAndValueNegated ? SymbolTable.symbolFor("-") : SymbolTable.symbolFor("+");
    AbstractMessageSendNode send =
        MessageSendNode.createGenericBinary(selector, getRcvr(),
            new IntegerLiteralNode(isMinusAndValueNegated ? -incValue : incValue),
            sourceCoord);

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
    return new IntUninitIncFieldNode(self, fieldIndex, coord, incValue);
  }

  public ExpressionNode createIncNode(final Local local, final int ctxLevel) {
    if (ctxLevel == 0) {
      return IntIncLocalVariableNodeGen.create(local, incValue).initialize(sourceCoord);
    }
    return IntIncNonLocalVariableNodeGen.create(ctxLevel, local, incValue)
                                        .initialize(sourceCoord);
  }
}
