package som.primitives;

import som.primitives.SystemPrims.BinarySystemNode;
import som.vmobjects.SObject;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.dsl.Specialization;

public abstract class HasGlobalPrim extends BinarySystemNode {
  @Specialization(guards = "receiverIsSystemObject")
  public final boolean doSObject(final SObject receiver, final SSymbol argument) {
    return universe.hasGlobal(argument);
  }
}
