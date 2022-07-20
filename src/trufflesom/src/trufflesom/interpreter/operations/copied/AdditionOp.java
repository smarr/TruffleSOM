package trufflesom.interpreter.operations.copied;

import com.oracle.truffle.api.bytecode.OperationProxy.Proxyable;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.primitives.arithmetic.AdditionPrim;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SObject;


@Proxyable
@ImportStatic(SymbolTable.class)
public abstract class AdditionOp extends AdditionPrim {

  @Specialization
  public static final Object doCached(final VirtualFrame frame, final SObject rcvr,
      final Object argument,
      @Cached("create(symPlus)") final AbstractDispatchNode dispatch) {
    return dispatch.executeDispatch(frame, new Object[] {rcvr, argument});
  }
}
