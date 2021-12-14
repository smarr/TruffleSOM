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
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.StandardTags.RootTag;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import bd.primitives.nodes.PreevaluatedExpression;
import tools.nodestats.Tags.AnyNode;


@GenerateWrapper
public abstract class ExpressionNode extends SOMNode implements InstrumentableNode {

  public abstract Object executeGeneric(VirtualFrame frame);

  public boolean isTrivial() {
    return false;
  }

  public boolean isTrivialInSequence() {
    return false;
  }

  public boolean isTrivialInBlock() {
    return isTrivial();
  }

  public PreevaluatedExpression copyTrivialNode() {
    throw new UnsupportedOperationException(
        "Some of the subclasses may be trivial and implement this");
  }

  @Override
  public ExpressionNode getFirstMethodBodyNode() {
    return this;
  }

  public boolean executeBoolean(final VirtualFrame frame) throws UnexpectedResultException {
    Object value = executeGeneric(frame);
    if (value instanceof Boolean) {
      return (boolean) value;
    }
    throw new UnexpectedResultException(value);
  }

  public long executeLong(final VirtualFrame frame) throws UnexpectedResultException {
    Object value = executeGeneric(frame);
    if (value instanceof Long) {
      return (long) value;
    }
    throw new UnexpectedResultException(value);
  }

  public double executeDouble(final VirtualFrame frame) throws UnexpectedResultException {
    Object value = executeGeneric(frame);
    if (value instanceof Double) {
      return (double) value;
    }
    throw new UnexpectedResultException(value);
  }

  @Override
  public boolean isInstrumentable() {
    return true;
  }

  @Override
  public WrapperNode createWrapper(final ProbeNode probe) {
    return new ExpressionNodeWrapper(this, probe);
  }

  @Override
  public boolean hasTag(final Class<? extends Tag> tag) {
    if (tag == AnyNode.class) {
      return true;
    }

    if (tag == RootTag.class) {
      Node parent = getParent();
      if (parent != null && parent.getParent() == null) {
        return true;
      }
    }
    return false;
  }
}
