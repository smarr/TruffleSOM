package trufflesom.interpreter.nodes.specialized;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import trufflesom.interpreter.nodes.nary.BinaryMsgExprNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SSymbol;


@GenerateNodeFactory
public abstract class AndBoolMessageNode extends BinaryMsgExprNode {
  @Specialization
  public final boolean doAnd(final VirtualFrame frame, final boolean receiver,
      final boolean argument) {
    return receiver && argument;
  }

  @Override
  public SSymbol getSelector() {
    // if (getSourceChar(0) == '&') {
    // return SymbolTable.symbolFor("&&");
    // }
    return SymbolTable.symbolFor("and:");
  }
}
