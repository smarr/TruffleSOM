package trufflesom.interpreter.supernodes.inc;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.compiler.Variable;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.Method.OpBuilder;
import trufflesom.interpreter.bc.RespecializeException;
import trufflesom.interpreter.nodes.AbstractMessageSendNode;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.FieldNode;
import trufflesom.interpreter.nodes.FieldNode.FieldReadNode;
import trufflesom.interpreter.nodes.GenericMessageSendNode;
import trufflesom.interpreter.nodes.LocalVariableNode;
import trufflesom.interpreter.nodes.MessageSendNode;
import trufflesom.interpreter.nodes.NonLocalVariableNode;
import trufflesom.interpreter.nodes.bc.BytecodeLoopNode;
import trufflesom.interpreter.nodes.literals.IntegerLiteralNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.VmSettings;
import trufflesom.vmobjects.SSymbol;


@NodeChild(value = "rcvr", type = ExpressionNode.class)
public abstract class IncExpWithValueNode extends ExpressionNode {
  protected final long    incValue;
  protected final boolean isMinusAndValueNegated;

  public IncExpWithValueNode(long incValue, boolean isMinusAndValueNegated) {
    this.incValue = incValue;
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
    if (rcvr instanceof FieldReadNode r) {
      return r.getFieldIndex() == fieldIdx;
    }

    return false;
  }

  public final boolean doesAccessVariable(final Variable var) {
    ExpressionNode rcvr = getRcvr();
    if (rcvr instanceof LocalVariableNode r) {
      return r.getLocal().equals(var);
    }

    if (rcvr instanceof NonLocalVariableNode nl) {
      return nl.getLocal().equals(var);
    }

    return false;
  }

  public final GenericMessageSendNode makeGenericSend() {
    CompilerDirectives.transferToInterpreterAndInvalidate();

    SSymbol selector = isMinusAndValueNegated ? SymbolTable.symMinus : SymbolTable.symPlus;
    GenericMessageSendNode send = MessageSendNode.createGeneric(selector,
        new ExpressionNode[] {getRcvr(),
            new IntegerLiteralNode(isMinusAndValueNegated ? -incValue : incValue)},
        sourceCoord);

    if (VmSettings.UseAstInterp) {
      replace(send);
      send.notifyDispatchInserted();
      return send;
    }

    assert getParent() instanceof BytecodeLoopNode : "This node was expected to be a direct child of a `BytecodeLoopNode`.";
    throw new RespecializeException(send);
  }

  public FieldNode createIncFieldNode(final ExpressionNode self, final byte fieldIndex,
      final long coord) {
    return new UninitIncFieldWithValueNode(self, fieldIndex, coord, incValue);
  }

  public final ExpressionNode createIncVarNode(final Local local, final int ctxLevel) {
    if (ctxLevel == 0) {
      return IncLocalVarWithValueNodeGen.create(local, incValue).initialize(sourceCoord);
    }
    return IncNonLocalVarWithValueNodeGen.create(ctxLevel, local, incValue)
                                         .initialize(sourceCoord);
  }

  @Override
  public void constructOperation(final OpBuilder opBuilder, boolean resultUsed) {
    opBuilder.dsl.beginIncExpWithValue(incValue);
    getRcvr().accept(opBuilder);
    opBuilder.dsl.endIncExpWithValue();
  }
}
