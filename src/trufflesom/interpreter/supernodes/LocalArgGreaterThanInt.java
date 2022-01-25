package trufflesom.interpreter.supernodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import bdt.inlining.ScopeAdaptationVisitor;
import bdt.inlining.ScopeAdaptationVisitor.ScopeElement;
import trufflesom.compiler.Variable.Argument;
import trufflesom.interpreter.bc.RespecializeException;
import trufflesom.interpreter.nodes.AbstractMessageSendNode;
import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgumentReadNode;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.MessageSendNode;
import trufflesom.interpreter.nodes.bc.BytecodeLoopNode;
import trufflesom.interpreter.nodes.literals.IntegerLiteralNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.VmSettings;


public final class LocalArgGreaterThanInt extends ExpressionNode {

  private final Argument arg;
  private final int      argIdx;
  private final long     intValue;

  public LocalArgGreaterThanInt(final Argument arg, final long intValue) {
    this.arg = arg;
    this.argIdx = arg.index;
    this.intValue = intValue;
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    Object arg = frame.getArguments()[argIdx];
    if (arg instanceof Long) {
      long argVal = (Long) arg;
      return argVal > intValue;
    }

    CompilerDirectives.transferToInterpreterAndInvalidate();
    return fallbackGeneric(frame);
  }

  @Override
  public Object doPreEvaluated(final VirtualFrame frame, final Object[] args) {
    CompilerDirectives.transferToInterpreterAndInvalidate();
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean executeBoolean(final VirtualFrame frame) throws UnexpectedResultException {
    Object arg = frame.getArguments()[argIdx];
    if (arg instanceof Long) {
      long argVal = (Long) arg;
      return argVal > intValue;
    }

    CompilerDirectives.transferToInterpreterAndInvalidate();
    return fallbackBool(frame);
  }

  private boolean fallbackBool(final VirtualFrame frame) throws UnexpectedResultException {
    Object result = makeGenericSend().doPreEvaluated(frame,
        new Object[] {arg, intValue});
    if (result instanceof Boolean) {
      return (Boolean) result;
    }
    throw new UnexpectedResultException(result);
  }

  private Object fallbackGeneric(final VirtualFrame frame) {
    return makeGenericSend().doPreEvaluated(frame,
        new Object[] {arg, intValue});
  }

  protected AbstractMessageSendNode makeGenericSend() {
    CompilerDirectives.transferToInterpreterAndInvalidate();
    AbstractMessageSendNode send =
        MessageSendNode.createGenericBinary(SymbolTable.symbolFor(">"),
            new LocalArgumentReadNode(arg),
            new IntegerLiteralNode(intValue), sourceCoord);

    if (VmSettings.UseAstInterp) {
      replace(send);
      send.notifyDispatchInserted();
      return send;
    }

    assert getParent() instanceof BytecodeLoopNode : "This node was expected to be a direct child of a `BytecodeLoopNode`.";
    throw new RespecializeException(send);
  }

  @Override
  public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
    ScopeElement<? extends Node> se = inliner.getAdaptedVar(arg);
    if (se.var != arg || se.contextLevel < 0) {
      if (se.var instanceof Argument) {
        replace(
            new LocalArgGreaterThanInt((Argument) se.var, intValue).initialize(sourceCoord));
      } else {
        replace(MessageSendNode.createGenericBinary(
            SymbolTable.symbolFor(">"),
            (ExpressionNode) se.var.getReadNode(se.contextLevel, sourceCoord),
            new IntegerLiteralNode(intValue),
            sourceCoord));
      }
    } else {
      assert 0 == se.contextLevel;
    }
  }
}
