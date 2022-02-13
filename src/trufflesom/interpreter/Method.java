/**
 * Copyright (c) 2013 Stefan Marr, stefan.marr@vub.ac.be
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package trufflesom.interpreter;

import java.util.Objects;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.source.Source;

import bd.inlining.Scope;
import bd.inlining.ScopeAdaptationVisitor;
import bd.primitives.nodes.PreevaluatedExpression;
import trufflesom.compiler.MethodGenerationContext;
import trufflesom.compiler.bc.BytecodeMethodGenContext;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.vmobjects.SInvokable.SMethod;


public final class Method extends Invokable {

  private final LexicalScope currentLexicalScope;

  @Child protected ExpressionNode body;

  protected final ExpressionNode uninitializedBody;

  public Method(final String name, final Source source, final long sourceCoord,
      final ExpressionNode expressions, final LexicalScope currentLexicalScope,
      final ExpressionNode uninitialized) {
    super(name, source, sourceCoord, currentLexicalScope.getFrameDescriptor());
    this.currentLexicalScope = currentLexicalScope;
    currentLexicalScope.setMethod(this);
    body = expressions;
    uninitializedBody = uninitialized;
  }

  @Override
  public Object execute(final VirtualFrame frame) {
    return body.executeGeneric(frame);
  }

  public LexicalScope getScope() {
    return currentLexicalScope;
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    }

    if (!(o instanceof Method)) {
      return false;
    }

    Method m = (Method) o;
    if (!m.name.equals(name)) {
      return false;
    }

    return sourceCoord == m.sourceCoord && source.equals(m.source);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, sourceCoord, source);
  }

  @Override
  public String toString() {
    return getName();
  }

  public Method cloneAndAdaptAfterScopeChange(final BytecodeMethodGenContext mgenc,
      final LexicalScope adaptedScope, final int appliesTo,
      final boolean cloneAdaptedAsUninitialized,
      final boolean requiresChangesToContextLevels) {
    Scope<?, ?> scope = mgenc == null ? adaptedScope : mgenc;
    ExpressionNode adaptedBody = ScopeAdaptationVisitor.adapt(uninitializedBody, scope,
        appliesTo, requiresChangesToContextLevels, getLanguage(SomLanguage.class));

    ExpressionNode uninit;
    if (cloneAdaptedAsUninitialized) {
      uninit = NodeUtil.cloneNode(adaptedBody);
    } else {
      uninit = uninitializedBody;
    }

    Method clone = new Method(name, source, sourceCoord, adaptedBody, adaptedScope, uninit);
    adaptedScope.setMethod(clone);
    return clone;
  }

  @Override
  public void propagateLoopCountThroughoutLexicalScope(final long count) {
    assert count >= 0;
    currentLexicalScope.propagateLoopCountThroughoutLexicalScope(count);
    LoopNode.reportLoopCount(this,
        (count > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) count);
  }

  @Override
  public Node deepCopy() {
    LexicalScope splitScope = currentLexicalScope.split();
    assert currentLexicalScope != splitScope;
    return cloneAndAdaptAfterScopeChange(null, splitScope, 0, false, false);
  }

  @Override
  public ExpressionNode inline(final MethodGenerationContext mgenc,
      final SMethod toBeInlined) {
    mgenc.mergeIntoScope(currentLexicalScope, toBeInlined);
    return ScopeAdaptationVisitor.adapt(uninitializedBody, mgenc, 0, true,
        getLanguage(SomLanguage.class));
  }

  @Override
  public boolean isTrivial() {
    if (currentLexicalScope.isBlock()) {
      return body.isTrivialInBlock();
    }
    return body.isTrivial();
  }

  @Override
  public PreevaluatedExpression copyTrivialNode() {
    return body.copyTrivialNode();
  }

  @Override
  public AbstractDispatchNode asDispatchNode(final Object rcvr,
      final AbstractDispatchNode next) {
    return body.asDispatchNode(rcvr, source, next);
  }
}
