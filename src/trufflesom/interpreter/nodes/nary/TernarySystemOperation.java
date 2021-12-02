package trufflesom.interpreter.nodes.nary;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import bd.primitives.nodes.WithContext;
import trufflesom.vm.Universe;


public abstract class TernarySystemOperation extends TernaryExpressionNode
    implements WithContext<TernarySystemOperation, Universe> {
  @CompilationFinal protected Universe universe;

  @Override
  public TernarySystemOperation initialize(final Universe universe) {
    assert this.universe == null && universe != null;
    this.universe = universe;
    return this;
  }
}
