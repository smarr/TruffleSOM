package bdt.testsetup;

import com.oracle.truffle.api.frame.VirtualFrame;

import bdt.inlining.TScopeBuilder;
import bdt.inlining.nodes.Inlinable;


public final class LambdaNode extends ExprNode implements Inlinable<TScopeBuilder> {

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    return null;
  }

  @Override
  public ExprNode inline(final TScopeBuilder scopeBuilder) {
    return new LambdaNode();
  }
}
