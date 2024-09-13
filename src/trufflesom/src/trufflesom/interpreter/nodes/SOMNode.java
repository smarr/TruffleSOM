/*
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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.instrumentation.InstrumentableNode.WrapperNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.bdt.basic.nodes.DummyParent;
import trufflesom.bdt.inlining.ScopeAdaptationVisitor;
import trufflesom.bdt.inlining.nodes.ScopeReference;
import trufflesom.bdt.inlining.nodes.WithSource;
import trufflesom.bdt.source.SourceCoordinate;
import trufflesom.interpreter.Method.OpBuilder;
import trufflesom.interpreter.Types;
import trufflesom.vm.NotYetImplementedException;


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
    return SourceCoordinate.createSourceSection(this, sourceCoord);
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

  @Override
  public Source getSource() {
    CompilerAsserts.neverPartOfCompilation();

    WithSource node = this;
    while (node != null && !node.hasSource()) {
      Node parent = ((Node) node).getParent();
      if (parent.getClass() == DummyParent.class) {
        return null;
      }

      while (!(parent instanceof WithSource)) {
        parent = parent.getParent();
      }
      node = (WithSource) parent;
    }

    if (node == null) {
      return null;
    }
    return node.getSource();
  }

  @Override
  public boolean hasSource() {
    return false;
  }

  public static Node getParentIgnoringWrapper(final Node node) {
    Node n = node.getParent();
    if (n instanceof WrapperNode) {
      return n.getParent();
    }
    return n;
  }

  @SuppressWarnings("unchecked")
  public static <N extends Node> N unwrapIfNeeded(final N node) {
    if (node instanceof WrapperNode) {
      return (N) ((WrapperNode) node).getDelegateNode();
    }
    return node;
  }

  public void constructOperation(@SuppressWarnings("unused") final OpBuilder opBuilder,
      boolean resultUsed) {
    throw new NotYetImplementedException("Class not yet supported: " + getClass().getName());
  }

  public void beginConstructOperation(@SuppressWarnings("unused") final OpBuilder opBuilder,
      boolean resultUsed) {
    throw new NotYetImplementedException("Class not yet supported: " + getClass().getName());
  }

  public void endConstructOperation(@SuppressWarnings("unused") final OpBuilder opBuilder,
      boolean resultUsed) {
    throw new NotYetImplementedException("Class not yet supported: " + getClass().getName());
  }
}
