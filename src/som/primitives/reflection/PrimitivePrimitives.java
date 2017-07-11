package som.primitives.reflection;

import som.primitives.Primitives;
import som.primitives.reflection.MethodPrimsFactory.HolderPrimFactory;
import som.primitives.reflection.MethodPrimsFactory.InvokeOnPrimFactory;
import som.primitives.reflection.MethodPrimsFactory.SignaturePrimFactory;
import som.vm.Universe;


public final class PrimitivePrimitives extends Primitives {
  public PrimitivePrimitives(final boolean displayWarning, final Universe uni) {
    super(displayWarning, uni);
  }

  @Override
  public void installPrimitives() {
    installInstancePrimitive("signature", SignaturePrimFactory.getInstance());
    installInstancePrimitive("holder", HolderPrimFactory.getInstance());
    installInstancePrimitive("invokeOn:with:", InvokeOnPrimFactory.getInstance());
  }
}
