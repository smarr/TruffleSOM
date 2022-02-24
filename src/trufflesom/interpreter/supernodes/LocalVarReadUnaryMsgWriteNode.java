package trufflesom.interpreter.supernodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import bdt.inlining.ScopeAdaptationVisitor;
import bdt.inlining.ScopeAdaptationVisitor.ScopeElement;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.nodes.LocalVariableNode;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.vmobjects.SSymbol;


public final class LocalVarReadUnaryMsgWriteNode extends LocalVariableNode {

  @Child AbstractDispatchNode dispatch;
  private final SSymbol       selector;

  public LocalVarReadUnaryMsgWriteNode(final Local local, final SSymbol selector) {
    super(local);
    assert selector.getNumberOfSignatureArguments() == 1;
    this.dispatch = new UninitializedDispatchNode(selector);
    this.selector = selector;
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    Object rcvr = frame.getObject(slotIndex);
    Object result = dispatch.executeDispatch(frame, new Object[] {rcvr});

    frame.setObject(slotIndex, result);
    return result;
  }

  @Override
  public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
    ScopeElement<? extends Node> se = inliner.getAdaptedVar(local);

    assert se.contextLevel == 0;
    if (se.var != local) {
      Node node =
          new LocalVarReadUnaryMsgWriteNode((Local) se.var, selector).initialize(sourceCoord);
      replace(node);
    } else {
      assert 0 == se.contextLevel;
    }
  }
}
