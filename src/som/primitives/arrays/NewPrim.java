package som.primitives.arrays;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;

import som.interpreter.nodes.ExpressionNode;
import som.primitives.Primitive;
import som.primitives.Specializer;
import som.primitives.basics.SystemPrims.BinarySystemNode;
import som.vm.Universe;
import som.vmobjects.SArray;
import som.vmobjects.SClass;


@GenerateNodeFactory
@Primitive(className = "Array", primitive = "new:", selector = "new:", classSide = true,
    inParser = false, specializer = NewPrim.IsArrayClass.class, requiresContext = true)
public abstract class NewPrim extends BinarySystemNode {

  public static class IsArrayClass extends Specializer<NewPrim> {
    public IsArrayClass(final Primitive prim, final NodeFactory<NewPrim> fact,
        final Universe universe) {
      super(prim, fact, universe);
    }

    @Override
    public boolean matches(final Object[] args, final ExpressionNode[] argNodes) {
      return args[0] == universe.arrayClass;
    }
  }

  public NewPrim(final SourceSection source, final Universe universe) {
    super(source, universe);
  }

  @Specialization(guards = "receiver == universe.arrayClass")
  public final SArray doSClass(final SClass receiver, final long length) {
    return new SArray(length);
  }
}
