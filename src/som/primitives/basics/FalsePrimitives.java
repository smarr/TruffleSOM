package som.primitives.basics;

import som.interpreter.nodes.specialized.NotMessageNodeFactory;
import som.primitives.Primitives;
import som.vm.Universe;


public final class FalsePrimitives extends Primitives {
  public FalsePrimitives(final boolean displayWarning, final Universe uni) {
    super(displayWarning, uni);
  }

  @Override
  public void installPrimitives() {
    installInstancePrimitive("not", NotMessageNodeFactory.getInstance());
    // installInstancePrimitive("ifTrue:", IfTrueMessageNodeFactory.getInstance());
    // installInstancePrimitive("ifFalse:", IfFalseMessageNodeFactory.getInstance());
  }
}
