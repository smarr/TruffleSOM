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
package som.interpreter.nodes;

import som.vmobjects.SObject;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

public abstract class FieldNode extends ExpressionNode {

  protected final int fieldIndex;

  protected FieldNode(final int fieldIndex, final SourceSection source) {
    super(source);
    this.fieldIndex = fieldIndex;
  }

  protected abstract ExpressionNode getSelf();

  @NodeChild(value = "self", type = ExpressionNode.class)
  public static abstract class FieldReadNode extends FieldNode
      implements PreevaluatedExpression {

    public FieldReadNode(final int fieldIndex, final SourceSection source) {
      super(fieldIndex, source);
    }

    public FieldReadNode(final FieldReadNode node) {
      this(node.fieldIndex, node.getSourceSection());
    }

    @Specialization
    public Object doObject(final SObject self) {
      return self.getField(fieldIndex);
    }

    @Override
    public Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return doObject((SObject) arguments[0]);
    }
  }

  @NodeChildren({
    @NodeChild(value = "self", type = ExpressionNode.class),
    @NodeChild(value = "value", type = ExpressionNode.class)})
  public abstract static class FieldWriteNode extends FieldNode
      implements PreevaluatedExpression {

    public FieldWriteNode(final int fieldIndex, final SourceSection source) {
      super(fieldIndex, source);
    }

    public FieldWriteNode(final FieldWriteNode node) {
      this(node.fieldIndex, node.getSourceSection());
    }

    @Override
    public final Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return doObject((SObject) arguments[0], arguments[1]);
    }

    @Specialization
    public Object doObject(final SObject self, final Object value) {
      self.setField(fieldIndex, value);
      return value;
    }
  }
}
