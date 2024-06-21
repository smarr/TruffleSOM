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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags.ExpressionTag;
import com.oracle.truffle.api.instrumentation.StandardTags.StatementTag;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.source.Source;

import trufflesom.bdt.primitives.nodes.PreevaluatedExpression;
import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgumentReadNode;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;


public final class SequenceNode extends NoPreEvalExprNode {
  @Children private final ExpressionNode[] expressions;

  public SequenceNode(final ExpressionNode[] expressions) {
    this.expressions = expressions;
  }

  @Override
  @ExplodeLoop
  public Object executeGeneric(final VirtualFrame frame) {
    int lastI = expressions.length - 1;
    for (int i = 0; i < lastI; i++) {
      expressions[i].executeGeneric(frame);
    }
    return expressions[lastI].executeGeneric(frame);
  }

  @Override
  public boolean isTrivial() {
    // has exactly two expressions
    if (expressions.length != 2) {
      return false;
    }

    // and the last/second one is the self return
    if (expressions[1].getClass() == LocalArgumentReadNode.class
        && ((LocalArgumentReadNode) expressions[1]).isSelfRead()) {
      return expressions[0].isTrivialInSequence();
    }

    return false;
  }

  @Override
  public boolean isTrivialInBlock() {
    // has exactly two expressions
    if (expressions.length != 2) {
      return false;
    }

    // and the last/second one is the self return
    if (expressions[1].getClass() == LocalArgumentReadNode.class
        && ((LocalArgumentReadNode) expressions[1]).isSelfRead()) {
      return expressions[0].isTrivialInSequenceInBlock();
    }

    return false;
  }

  @Override
  public PreevaluatedExpression copyTrivialNode() {
    if (isTrivial()) {
      return expressions[0].copyTrivialNode();
    }
    return null;
  }

  @Override
  public PreevaluatedExpression copyTrivialNodeInBlock() {
    if (isTrivialInBlock()) {
      return expressions[0].copyTrivialNodeInBlock();
    }
    return null;
  }

  @Override
  public AbstractDispatchNode asDispatchNode(final Object rcvr, final Source source,
      final AbstractDispatchNode next) {
    if (isTrivial()) {
      return expressions[0].asDispatchNode(rcvr, source, next);
    }
    return null;
  }

  @Override
  public boolean hasTag(final Class<? extends Tag> tag) {
    if (tag == StatementTag.class || tag == ExpressionTag.class) {
      return false;
    }
    return super.hasTag(tag);
  }
}
