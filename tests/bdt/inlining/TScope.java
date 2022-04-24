package bdt.inlining;

import bdt.inlining.Scope;
import bdt.inlining.Variable;

public class TScope implements Scope<TScope, Void> {

  @Override
  @SuppressWarnings("unchecked")
  public Variable<?>[] getVariables() {
    return null;
  }

  @Override
  public TScope getOuterScopeOrNull() {
    return null;
  }

  @Override
  public TScope getScope(final Void method) {
    return null;
  }

  @Override
  public String getName() {
    return null;
  }
}
