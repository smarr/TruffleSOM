package trufflesom.interpreter.supernodes.compare;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import trufflesom.interpreter.bc.RespecializeException;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.GenericMessageSendNode;
import trufflesom.interpreter.nodes.MessageSendNode;
import trufflesom.interpreter.nodes.bc.BytecodeLoopNode;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.literals.IntegerLiteralNode;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.interpreter.nodes.nary.UnaryMsgExprNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.VmSettings;
import trufflesom.vmobjects.SSymbol;


public abstract class GreaterThanIntNode extends UnaryExpressionNode {
  private final long intValue;

  public GreaterThanIntNode(final long intValue) {
    this.intValue = intValue;
  }

  public SSymbol getSelector() {
    return SymbolTable.symbolFor(">");
  }

  @Override
  public abstract ExpressionNode getReceiver();

  @Specialization
  public final boolean doLong(final long rcvr) {
    return rcvr > intValue;
  }

  @Specialization
  public final boolean doDouble(final double rcvr) {
    return rcvr > intValue;
  }

  @Fallback
  public final Object genericSend(final VirtualFrame frame,
      final Object receiver,
      @Cached("create(getSelector())") final AbstractDispatchNode dispatch) {
    return dispatch.executeDispatch(frame, new Object[] {receiver, intValue});
  }
}
