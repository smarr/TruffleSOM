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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.StandardTags.ExpressionTag;
import com.oracle.truffle.api.instrumentation.StandardTags.RootTag;
import com.oracle.truffle.api.instrumentation.StandardTags.StatementTag;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.Source;

import bdt.primitives.nodes.PreevaluatedExpression;
import tools.nodestats.Tags.AnyNode;
import trufflesom.interpreter.nodes.ReturnNonLocalNode.CatchNonLocalReturnNode;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.vm.VmSettings;


@GenerateWrapper
public abstract class ExpressionNode extends SOMNode
    implements InstrumentableNode, PreevaluatedExpression {

  public abstract Object executeGeneric(VirtualFrame frame);

  @Override
  public abstract Object doPreEvaluated(VirtualFrame frame, Object[] args);

  public boolean isTrivial() {
    return false;
  }

  public boolean isTrivialInSequence() {
    return false;
  }

  public boolean isTrivialInBlock() {
    return isTrivial();
  }

  public boolean isTrivialInSequenceInBlock() {
    return false;
  }

  public PreevaluatedExpression copyTrivialNode() {
    // Some of the subclasses may be trivial and implement this
    return null;
  }

  public PreevaluatedExpression copyTrivialNodeInSequence() {
    // Some of the subclasses may be trivial and implement this
    return null;
  }

  public PreevaluatedExpression copyTrivialNodeInBlock() {
    return copyTrivialNode();
  }

  public PreevaluatedExpression copyTrivialNodeInSequenceInBlock() {
    // Some of the subclasses may be trivial and implement this
    return null;
  }

  public AbstractDispatchNode asDispatchNode(final Object rcvr, final Source source,
      final AbstractDispatchNode next) {
    // Some of the subclasses may be trivial and implement this
    return null;
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
    CompilerDirectives.transferToInterpreterAndInvalidate();
    throw new UnexpectedResultException(value);
  }

  public long executeLong(final VirtualFrame frame) throws UnexpectedResultException {
    Object value = executeGeneric(frame);
    if (value instanceof Long) {
      return (long) value;
    }
    CompilerDirectives.transferToInterpreterAndInvalidate();
    throw new UnexpectedResultException(value);
  }

  public double executeDouble(final VirtualFrame frame) throws UnexpectedResultException {
    Object value = executeGeneric(frame);
    if (value instanceof Double) {
      return (double) value;
    }
    CompilerDirectives.transferToInterpreterAndInvalidate();
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
      if (parent instanceof WrapperNode) {
        parent = parent.getParent();
      }

      if (parent.getClass() == CatchNonLocalReturnNode.class) {
        return true;
      }
      if (parent != null) {
        Node grandParent = parent.getParent();
        if (grandParent == null) {
          return true;
        }
        if (grandParent instanceof WrapperNode && grandParent.getParent() == null) {
          return true;
        }
      }
      return false;
    }

    if (tag == StatementTag.class || tag == ExpressionTag.class) {
      return true;
    }
    return false;
  }

  public void notifyAsInserted() {
    if (VmSettings.UseInstrumentation) {
      notifyInserted(this);
    }
  }
}
