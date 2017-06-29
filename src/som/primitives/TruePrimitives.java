package som.primitives;

import som.interpreter.nodes.specialized.NotMessageNodeFactory;
import som.vm.Universe;


public final class TruePrimitives extends Primitives {
  public TruePrimitives(final boolean displayWarning, final Universe uni) {
    super(displayWarning, uni);
  }

  @Override
  public void installPrimitives() {
    installInstancePrimitive("not", NotMessageNodeFactory.getInstance());
    // installInstancePrimitive("ifTrue:", IfTrueMessageNodeFactory.getInstance());
    // installInstancePrimitive("ifFalse:", IfFalseMessageNodeFactory.getInstance());
  }
}
