package trufflesom.primitives.arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import bd.primitives.Primitive;
import bd.primitives.Specializer;
import trufflesom.interpreter.SomLanguage;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.MessageSendNode;
import trufflesom.interpreter.nodes.MessageSendNode.GenericMessageSendNode;
import trufflesom.interpreter.nodes.nary.BinaryExpressionNode.BinarySystemOperation;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SSymbol;


@Primitive(className = "Array", primitive = "new:", selector = "new:", classSide = true,
    inParser = false, specializer = NewPrim.IsArrayClass.class, noWrapper = true)
public abstract class NewPrim extends BinarySystemOperation {

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

  @Fallback
  public final Object makeGenericSend(final VirtualFrame frame,
      final Object receiver, final Object argument) {
    return makeGenericSend().doPreEvaluated(frame,
        new Object[] {receiver, argument});
  }

  private GenericMessageSendNode makeGenericSend() {
    CompilerDirectives.transferToInterpreterAndInvalidate();
    GenericMessageSendNode node = MessageSendNode.createGeneric(SymbolTable.symbolFor("new:"),
        new ExpressionNode[] {getReceiver(), getArgument()}, sourceSection,
        SomLanguage.getCurrentContext());
    return replace(node);
  }
}
