package trufflesom.interpreter.nodes.nary;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;

import bd.primitives.nodes.WithContext;
import trufflesom.vm.Universe;


@GenerateNodeFactory
public abstract class BinarySystemOperation extends BinaryExpressionNode
    implements WithContext<BinarySystemOperation, Universe> {
  @CompilationFinal protected Universe universe;

  @Override
  public BinarySystemOperation initialize(final Universe universe) {
    assert this.universe == null && universe != null;
    this.universe = universe;
    return this;
  }
}
