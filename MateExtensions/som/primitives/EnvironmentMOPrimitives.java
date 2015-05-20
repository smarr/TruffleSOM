package som.primitives;

import som.primitives.MatePrimsFactory.MateNewEnvironmentPrimFactory;
import som.primitives.Primitives;

public class EnvironmentMOPrimitives extends Primitives {

  public EnvironmentMOPrimitives(final boolean displayWarning) {
    super(displayWarning); 
  }

  @Override
  public void installPrimitives() {
    installClassPrimitive("new", MateNewEnvironmentPrimFactory.getInstance());
  }
}
