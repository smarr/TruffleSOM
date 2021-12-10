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
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import bd.basic.nodes.DummyParent;
import bd.inlining.ScopeAdaptationVisitor;
import bd.inlining.nodes.ScopeReference;
import bd.inlining.nodes.WithSource;
import bd.source.SourceCoordinate;
import trufflesom.interpreter.Types;


@TypeSystemReference(Types.class)
public abstract class SOMNode extends Node implements ScopeReference, WithSource {

  @CompilationFinal protected long sourceCoord;

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Node> T initialize(final long coord) {
    assert coord != 0;
    assert this.sourceCoord == 0 : "sourceSection should only be set once";
    this.sourceCoord = coord;
    return (T) this;
  }

  @Override
  public SourceSection getSourceSection() {
    RootNode root = getRootNode();
    assert root != null;
    SourceSection section = root.getSourceSection();
    if (section == null) {
      assert root instanceof DummyParent : "We should have a source section, "
          + "except if the root is a DummyParent during inlining";
      return null;
    }
    return SourceCoordinate.createSourceSection(section.getSource(), sourceCoord);
  }

  @Override
  public long getSourceCoordinate() {
    return sourceCoord;
  }

  protected final char getSourceChar(final int offsetFromStart) {
    Source source = getRootNode().getSourceSection().getSource();
    int index = SourceCoordinate.getStartIndex(sourceCoord);
    return source.getCharacters().charAt(index + offsetFromStart);
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
