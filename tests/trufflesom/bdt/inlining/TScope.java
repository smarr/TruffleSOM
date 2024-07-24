package trufflesom.bdt.inlining;

import com.oracle.truffle.api.nodes.Node;
import trufflesom.compiler.Variable;


public class TScope implements Scope<TScope, Void> {

  @Override
  public Variable[] getVariables() {
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
