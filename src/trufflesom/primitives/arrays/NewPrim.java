package trufflesom.primitives.arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import bd.primitives.Primitive;
import bd.primitives.Specializer;
import trufflesom.interpreter.SomLanguage;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.nary.BinarySystemMsgOperation;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SSymbol;


@Primitive(className = "Array", primitive = "new:", selector = "new:", classSide = true,
    inParser = false, specializer = NewPrim.IsArrayClass.class)
public abstract class NewPrim extends BinarySystemMsgOperation {

  public static class IsArrayClass extends Specializer<Universe, ExpressionNode, SSymbol> {
    @CompilationFinal private Universe universe;

    public IsArrayClass(final Primitive prim, final NodeFactory<ExpressionNode> fact) {
      super(prim, fact);
    }

    @Override
    public boolean matches(final Object[] args, final ExpressionNode[] argNodes) {
      if (universe == null) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        universe = SomLanguage.getCurrentContext();
      }
      return args[0] == universe.arrayClass;
    }
  }

  @Specialization(guards = "receiver == universe.arrayClass")
  public final SArray doSClass(final SClass receiver, final long length) {
    return new SArray(length);
  }

  @Override
  public SSymbol getSelector() {
    return SymbolTable.symNewMsg;
  }
}
