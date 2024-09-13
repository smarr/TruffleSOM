package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.bytecode.OperationProxy.Proxyable;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import trufflesom.interpreter.Method.OpBuilder;
import trufflesom.interpreter.nodes.nary.BinaryMsgExprNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SSymbol;


@Proxyable
@GenerateNodeFactory
public abstract class AndBoolMessageNode extends BinaryMsgExprNode {
  @Specialization
  public static final boolean doAnd(final boolean receiver, final boolean argument) {
    return receiver && argument;
  }

  @Override
  public SSymbol getSelector() {
    if (getSourceChar(0) == '&') {
      return SymbolTable.symbolFor("&&");
    }
    return SymbolTable.symbolFor("and:");
  }

  @Override
  public void constructOperation(final OpBuilder opBuilder, boolean resultUsed) {
    opBuilder.dsl.beginAndBoolMessage();
    getReceiver().accept(opBuilder);
    getArgument().accept(opBuilder);
    opBuilder.dsl.endAndBoolMessage();
  }
}
