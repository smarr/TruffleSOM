package trufflesom.primitives.arrays;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import trufflesom.bdt.primitives.Primitive;
import trufflesom.bdt.primitives.Specializer;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.nary.BinaryMsgExprNode;
import trufflesom.vm.Classes;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SSymbol;


@ImportStatic(Classes.class)
@GenerateNodeFactory
@Primitive(className = "Array", primitive = "new:", selector = "new:", classSide = true,
    inParser = false, specializer = NewPrim.IsArrayClass.class)
public abstract class NewPrim extends BinaryMsgExprNode {

  public static class IsArrayClass extends Specializer<ExpressionNode, SSymbol> {

    public IsArrayClass(final Primitive prim, final NodeFactory<ExpressionNode> fact) {
      super(prim, fact);
    }

    @Override
    public boolean matches(final Object[] args, final ExpressionNode[] argNodes) {
      return args[0] == Classes.arrayClass;
    }
  }

  @Specialization(guards = "receiver == arrayClass")
  public static final SArray doSClass(@SuppressWarnings("unused") final SClass receiver,
      final long length) {
    return new SArray(length);
  }

  @Override
  public SSymbol getSelector() {
    return SymbolTable.symNewMsg;
  }
}
