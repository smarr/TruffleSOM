package trufflesom.interpreter.nodes.supernodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import bd.inlining.ScopeAdaptationVisitor;
import bd.inlining.ScopeAdaptationVisitor.ScopeElement;
import trufflesom.compiler.Variable.Argument;
import trufflesom.interpreter.bc.RespecializeException;
import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgumentReadNode;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.FieldNode.FieldReadNode;
import trufflesom.interpreter.nodes.GenericMessageSendNode;
import trufflesom.interpreter.nodes.MessageSendNode;
import trufflesom.interpreter.nodes.bc.BytecodeLoopNode;
import trufflesom.interpreter.nodes.literals.GenericLiteralNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.UninitializedReadFieldNode;
import trufflesom.interpreter.objectstorage.ObjectLayout;
import trufflesom.interpreter.objectstorage.StorageLocation;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.VmSettings;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SObject;


public final class LocalFieldStringEqualsNode extends ExpressionNode {

  private final int        fieldIdx;
  private final String     value;
  protected final Argument arg;

  @Child private AbstractReadFieldNode readFieldNode;

  @CompilationFinal private int state;

  public LocalFieldStringEqualsNode(final int fieldIdx, final Argument arg,
      final String value) {
    this.fieldIdx = fieldIdx;
    this.arg = arg;
    this.value = value;

    this.state = 0;
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    try {
      SObject rcvr = (SObject) frame.getArguments()[0];
      return executeEvaluated(frame, rcvr);
    } catch (UnexpectedResultException e) {
      return e.getResult();
    }
  }

  @Override
  public Object doPreEvaluated(final VirtualFrame frame, final Object[] args) {
    try {
      return executeEvaluated(frame, (SObject) args[0]);
    } catch (UnexpectedResultException e) {
      return e.getResult();
    }
  }

  public boolean executeEvaluated(final VirtualFrame frame, final SObject rcvr)
      throws UnexpectedResultException {
    int currentState = state;

    if (state == 0) {
      // uninitialized
      CompilerDirectives.transferToInterpreterAndInvalidate();
      final ObjectLayout layout = rcvr.getObjectLayout();
      StorageLocation location = layout.getStorageLocation(fieldIdx);

      readFieldNode =
          insert(location.getReadNode(fieldIdx, layout,
              new UninitializedReadFieldNode(fieldIdx)));
    }

    Object result = readFieldNode.read(rcvr);

    if ((state & 0b1) != 0) {
      // we saw a string before
      if (result instanceof String) {
        return ((String) result).equals(value);
      }
    }

    if ((state & 0b10) != 0) {
      // we saw a nil before
      if (result == Nil.nilObject) {
        return false;
      }
    }

    CompilerDirectives.transferToInterpreterAndInvalidate();
    return specialize(frame, result, currentState);
  }

  @Override
  public boolean executeBoolean(final VirtualFrame frame) throws UnexpectedResultException {
    SObject rcvr = (SObject) frame.getArguments()[0];

    return executeEvaluated(frame, rcvr);
  }

  private boolean specialize(final VirtualFrame frame, final Object result,
      final int currentState) throws UnexpectedResultException {
    if (result instanceof String) {
      state = currentState | 0b1;
      return value.equals(result);
    }

    if (result == Nil.nilObject) {
      state = currentState | 0b10;
      return false;
    }

    Object sendResult =
        makeGenericSend(result).doPreEvaluated(frame, new Object[] {result, value});
    if (sendResult instanceof Boolean) {
      return (Boolean) sendResult;
    }
    throw new UnexpectedResultException(sendResult);
  }

  public final GenericMessageSendNode makeGenericSend(final Object receiver) {
    ExpressionNode[] children;
    if (VmSettings.UseAstInterp) {
      children =
          new ExpressionNode[] {new FieldReadNode(new LocalArgumentReadNode(arg), fieldIdx),
              new GenericLiteralNode(value)};
    } else {
      children = null;
    }

    GenericMessageSendNode send =
        MessageSendNode.createGeneric(SymbolTable.symbolFor("="), children, sourceCoord);

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
      Node newNode;
      if (se.contextLevel == 0) {
        newNode =
            new LocalFieldStringEqualsNode(fieldIdx, (Argument) se.var, value).initialize(
                fieldIdx);
      } else {
        newNode = new NonLocalFieldStringEqualsNode(fieldIdx, (Argument) se.var,
            se.contextLevel, value).initialize(fieldIdx);
      }

      replace(newNode);
    } else {
      assert 0 == se.contextLevel;
    }
  }
}
