package som.primitives.arrays;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import som.primitives.SystemPrims.BinarySystemNode;
import som.vm.Universe;
import som.vmobjects.SArray;
import som.vmobjects.SClass;


@GenerateNodeFactory
public abstract class NewPrim extends BinarySystemNode {

  public NewPrim(final Universe universe) {
    super(universe);
  }

  @Specialization(guards = "receiver == universe.arrayClass")
  public final SArray doSClass(final SClass receiver, final long length) {
    return new SArray(length);
  }
}
