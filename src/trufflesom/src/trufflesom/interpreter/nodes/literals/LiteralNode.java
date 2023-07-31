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
package trufflesom.interpreter.nodes.literals;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.source.Source;

import trufflesom.bdt.inlining.nodes.Inlinable;
import trufflesom.bdt.primitives.nodes.PreevaluatedExpression;
import trufflesom.compiler.MethodGenerationContext;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.GlobalNode.FalseGlobalNode;
import trufflesom.interpreter.nodes.GlobalNode.NilGlobalNode;
import trufflesom.interpreter.nodes.GlobalNode.TrueGlobalNode;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.CachedLiteralNode;
import trufflesom.interpreter.nodes.dispatch.DispatchGuard;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SBlock;


@NodeInfo(cost = NodeCost.NONE)
public abstract class LiteralNode extends ExpressionNode
    implements PreevaluatedExpression, Inlinable<MethodGenerationContext> {
  public static ExpressionNode create(final Object literal) {
    if (literal instanceof SBlock) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      throw new IllegalArgumentException(
          "SBlock isn't supported here, BlockNodes need to be constructed directly.");
    }

    if (literal instanceof Long) {
      return new IntegerLiteralNode((Long) literal);
    }

    if (literal instanceof Double) {
      return new DoubleLiteralNode((Double) literal);
    }

    if (literal == Boolean.TRUE) {
      return new TrueGlobalNode(null);
    }

    if (literal == Boolean.FALSE) {
      return new FalseGlobalNode(null);
    }

    if (literal == Nil.nilObject) {
      return new NilGlobalNode(null);
    }

    return new GenericLiteralNode(literal);
  }

  @Override
  public ExpressionNode inline(final MethodGenerationContext mgenc) {
    return this;
  }

  @Override
  public boolean isTrivial() {
    return true;
  }

  @Override
  public PreevaluatedExpression copyTrivialNode() {
    return (PreevaluatedExpression) copy();
  }

  @Override
  public AbstractDispatchNode asDispatchNode(final Object rcvr, final Source source,
      final AbstractDispatchNode next) {
    return new CachedLiteralNode(DispatchGuard.create(rcvr), source, executeGeneric(null),
        next);
  }
}
