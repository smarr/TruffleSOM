package trufflesom.bdt.inlining;

import com.oracle.truffle.api.nodes.Node;


public class TScope implements Scope<TScope, Void> {

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Variable<? extends Node>> T[] getVariables() {
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
