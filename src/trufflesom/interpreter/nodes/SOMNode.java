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
package trufflesom.interpreter.nodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;

import bd.inlining.ScopeAdaptationVisitor;
import bd.inlining.nodes.ScopeReference;
import bd.inlining.nodes.WithSource;
import trufflesom.interpreter.Types;


@TypeSystemReference(Types.class)
public abstract class SOMNode extends Node implements ScopeReference, WithSource {

  @CompilationFinal protected SourceSection sourceSection;

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Node> T initialize(final SourceSection sourceSection) {
    assert sourceSection != null;
    assert this.sourceSection == null : "sourceSection should only be set once";
    this.sourceSection = sourceSection;
    return (T) this;
  }

  @Override
  public SourceSection getSourceSection() {
    return sourceSection;
  }

  protected final char getSourceChar(final int offsetFromStart) {
    return sourceSection.getSource().getCharacters()
                        .charAt(sourceSection.getCharIndex() + offsetFromStart);
  }

  @Override
  public void replaceAfterScopeChange(final ScopeAdaptationVisitor inliner) {
    // do nothing!
    // only a small subset of nodes needs to implement this method.
    // Most notably, nodes using FrameSlots, and block nodes with method nodes.
  }

  /**
   * @return body of a node that just wraps the actual method body.
   */
  public abstract ExpressionNode getFirstMethodBodyNode();
}
