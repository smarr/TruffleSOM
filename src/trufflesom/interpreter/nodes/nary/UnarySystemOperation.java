package trufflesom.interpreter.nodes.nary;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import bd.primitives.nodes.WithContext;
import trufflesom.vm.Universe;


public abstract class UnarySystemOperation extends UnaryExpressionNode
    implements WithContext<UnarySystemOperation, Universe> {
  @CompilationFinal protected Universe universe;

  @Override
  public UnarySystemOperation initialize(final Universe universe) {
    assert this.universe == null && universe != null;
    this.universe = universe;
    return this;
  }
}
