package trufflesom.interpreter.supernodes;

import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import bdt.inlining.ScopeAdaptationVisitor;
import bdt.inlining.ScopeAdaptationVisitor.ScopeElement;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.nodes.NonLocalVariableNode;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.vmobjects.SSymbol;


public final class NonLocalVarReadUnaryMsgWriteNode extends NonLocalVariableNode {

  @Child AbstractDispatchNode dispatch;
  private final SSymbol       selector;

  public NonLocalVarReadUnaryMsgWriteNode(final int contextLevel, final Local local,
      final SSymbol selector) {
    super(contextLevel, local);
    assert selector.getNumberOfSignatureArguments() == 1;
    this.dispatch = new UninitializedDispatchNode(selector);
    this.selector = selector;
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    MaterializedFrame ctx = determineContext(frame);
    Object rcvr = ctx.getObject(slotIndex);
    Object result = dispatch.executeDispatch(frame, new Object[] {rcvr});

    ctx.setObject(slotIndex, result);
    return result;
  }

  @Override
  public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
    ScopeElement<? extends Node> se = inliner.getAdaptedVar(local);

    if (se.var != local || se.contextLevel < contextLevel) {
      Node node;
      if (se.contextLevel > 0) {
        node = new NonLocalVarReadUnaryMsgWriteNode(se.contextLevel, (Local) se.var, selector);
      } else {
        assert se.contextLevel == 0;
        node = new LocalVarReadUnaryMsgWriteNode((Local) se.var, selector);
      }

      replace(node);
    } else {
      assert contextLevel == se.contextLevel;
    }
  }
}
