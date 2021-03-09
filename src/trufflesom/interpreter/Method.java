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

import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.source.SourceSection;

import bd.inlining.ScopeAdaptationVisitor;
import trufflesom.compiler.MethodGenerationContext;
import trufflesom.compiler.Parser.ParseError;
import trufflesom.compiler.bc.BytecodeMethodGenContext;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.vmobjects.SInvokable.SMethod;


public final class Method extends Invokable {

  private final LexicalScope currentLexicalScope;

  public Method(final String name, final SourceSection sourceSection,
      final ExpressionNode expressions, final LexicalScope currentLexicalScope,
      final ExpressionNode uninitialized, final SomLanguage lang) {
    super(name, sourceSection, currentLexicalScope.getFrameDescriptor(), expressions,
        uninitialized, lang);
    this.currentLexicalScope = currentLexicalScope;
    currentLexicalScope.setMethod(this);
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

    return m.sourceSection.equals(sourceSection);
  }

  @Override
  public String toString() {
    return "Method " + getName() + " @" + Integer.toHexString(hashCode());
  }

  public Method cloneAndAdaptAfterScopeChange(final LexicalScope adaptedScope,
      final int appliesTo, final boolean cloneAdaptedAsUninitialized,
      final boolean outerScopeChanged) {
    ExpressionNode adaptedBody = ScopeAdaptationVisitor.adapt(uninitializedBody, adaptedScope,
        appliesTo, outerScopeChanged, getLanguage(SomLanguage.class));

    ExpressionNode uninit;
    if (cloneAdaptedAsUninitialized) {
      uninit = NodeUtil.cloneNode(adaptedBody);
    } else {
      uninit = uninitializedBody;
    }

    Method clone = new Method(name, sourceSection, adaptedBody, adaptedScope, uninit,
        getLanguage(SomLanguage.class));
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
    return cloneAndAdaptAfterScopeChange(splitScope, 0, false, true);
  }

  @Override
  public ExpressionNode inline(final MethodGenerationContext mgenc,
      final SMethod toBeInlined) {
    mgenc.mergeIntoScope(currentLexicalScope, toBeInlined);
    return ScopeAdaptationVisitor.adapt(uninitializedBody, mgenc.getCurrentLexicalScope(), 0,
        true, getLanguage(SomLanguage.class));
  }

  @Override
  public void inlineBytecode(final BytecodeMethodGenContext mgenc, final SMethod toBeInlined)
      throws ParseError {
    mgenc.mergeIntoScope(currentLexicalScope, toBeInlined);
    getBodyForInlining().inlineInto(mgenc);
  }
}
