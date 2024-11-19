package trufflesom.interpreter.supernodes.compare;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import trufflesom.bdt.inlining.ScopeAdaptationVisitor;
import trufflesom.bdt.inlining.ScopeAdaptationVisitor.ScopeElement;
import trufflesom.compiler.Variable.Argument;
import trufflesom.interpreter.Method.OpBuilder;
import trufflesom.interpreter.bc.RespecializeException;
import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgumentReadNode;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.GenericMessageSendNode;
import trufflesom.interpreter.nodes.MessageSendNode;
import trufflesom.interpreter.nodes.bc.BytecodeLoopNode;
import trufflesom.interpreter.nodes.literals.IntegerLiteralNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.VmSettings;


public final class LocalArgLessThanInt extends ExpressionNode {
  private final Argument arg;
  private final int      argIdx;
  private final long     intValue;

  public LocalArgLessThanInt(final Argument arg, final long intValue) {
    this.arg = arg;
    this.argIdx = arg.index;
    this.intValue = intValue;
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    Object arg = frame.getArguments()[argIdx];
    if (arg instanceof Long) {
      long argVal = (Long) arg;
      return argVal < intValue;
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
      return argVal < intValue;
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
    return makeGenericSend().doPreEvaluated(frame, new Object[] {arg, intValue});
  }

  protected GenericMessageSendNode makeGenericSend() {
    CompilerDirectives.transferToInterpreterAndInvalidate();
    GenericMessageSendNode send =
        MessageSendNode.createGeneric(SymbolTable.symbolFor("<"), new ExpressionNode[] {
            new LocalArgumentReadNode(arg), new IntegerLiteralNode(intValue)}, sourceCoord);

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
    ScopeElement se = inliner.getAdaptedVar(arg);
    if (se.var != arg || se.contextLevel < 0) {
      if (se.var instanceof Argument a) {
        replace(new LocalArgLessThanInt(a, intValue).initialize(sourceCoord));
      } else {
        replace(LessThanIntNodeGen.create(intValue, se.var.getReadNode(se.contextLevel, sourceCoord)));
      }
    } else {
      assert 0 == se.contextLevel;
    }
  }

  @Override
  public void constructOperation(final OpBuilder opBuilder, boolean resultUsed) {
    if (resultUsed) {
      opBuilder.dsl.beginLessThanPrim();
      opBuilder.dsl.emitLoadArgument(argIdx);
      opBuilder.dsl.emitLoadConstant(intValue);
      opBuilder.dsl.endLessThanPrim();
    }
  }
}
