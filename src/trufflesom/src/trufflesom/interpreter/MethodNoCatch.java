package trufflesom.interpreter;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.bdt.inlining.Scope;
import trufflesom.interpreter.nodes.ExpressionNode;


public final class MethodNoCatch extends Method {
  public MethodNoCatch(final String name, final Source source, final long sourceCoord,
      final ExpressionNode expressions, final LexicalScope currentLexicalScope,
      final ExpressionNode uninitialized) {
    super(name, source, sourceCoord, expressions, currentLexicalScope, uninitialized);
  }

  @Override
  public Object execute(final VirtualFrame frame) {
    return body.executeGeneric(frame);
  }

  @Override
  protected Method createAdapted(final ExpressionNode adaptedBody,
      final Scope<?, ?> newScope, final LexicalScope adaptedScope,
      final ExpressionNode uninit) {
    return new MethodNoCatch(name, source, sourceCoord, adaptedBody, adaptedScope, uninit);
  }
}
