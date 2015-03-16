package som.primitives;

import som.primitives.SystemPrims.BinarySystemNode;
import som.vm.constants.Nil;
import som.vmobjects.SObject;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

public abstract class GlobalPrim extends BinarySystemNode {
  @Specialization(guards = "receiverIsSystemObject")
  public final Object doSObject(final VirtualFrame frame, final SObject receiver, final SSymbol argument) {
    Object result = universe.getGlobal(argument);
    return result != null ? result : Nil.nilObject;
  }
}
