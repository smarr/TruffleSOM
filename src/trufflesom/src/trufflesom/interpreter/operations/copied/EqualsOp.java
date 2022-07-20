package trufflesom.interpreter.operations.copied;

import com.oracle.truffle.api.bytecode.OperationProxy.Proxyable;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.primitives.basics.EqualsPrim;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


@Proxyable
public abstract class EqualsOp extends EqualsPrim {

  public static final SSymbol symEqual = SymbolTable.symbolFor("=");

  @Specialization
  public static final Object doCached(final VirtualFrame frame, final SObject rcvr,
      final Object argument,
      @Cached("create(symEqual)") final AbstractDispatchNode dispatch) {
    return dispatch.executeDispatch(frame, new Object[] {rcvr, argument});
  }
}
