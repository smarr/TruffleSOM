package trufflesom.interpreter.supernodes.compare;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.interpreter.Method.OpBuilder;
import trufflesom.interpreter.bc.RespecializeException;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.GenericMessageSendNode;
import trufflesom.interpreter.nodes.MessageSendNode;
import trufflesom.interpreter.nodes.bc.BytecodeLoopNode;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.literals.GenericLiteralNode;
import trufflesom.interpreter.nodes.nary.UnaryExpressionNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.VmSettings;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SSymbol;


public abstract class StringEqualsNode extends UnaryExpressionNode {
  private final String value;

  protected static final Object nil = Nil.nilObject;

  protected StringEqualsNode(final String value) {
    this.value = value;
  }

  public SSymbol getSelector() {
    return SymbolTable.symbolFor("=");
  }

  @Override
  public abstract ExpressionNode getReceiver();

  @Specialization
  public final boolean doString(final String rcvr) {
    return value.equals(rcvr);
  }

  @Specialization(guards = "rcvr == nil")
  public static final boolean doNil(@SuppressWarnings("unused") final Object rcvr) {
    return false;
  }

  @Fallback
  public final Object genericSend(final VirtualFrame frame,
      final Object receiver,
      @Cached("create(getSelector())") final AbstractDispatchNode dispatch) {
    return dispatch.executeDispatch(frame, new Object[] {receiver, value});
  }

  @Override
  public void constructOperation(final OpBuilder opBuilder) {
    opBuilder.dsl.beginEqualsOp();
    opBuilder.dsl.emitLoadConstant(value);
    getReceiver().accept(opBuilder);
    opBuilder.dsl.endEqualsOp();
  }
}
